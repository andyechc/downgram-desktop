import asyncio
import uvicorn
import argparse
import time
from fastapi import FastAPI, HTTPException, WebSocket, WebSocketDisconnect
from pydantic import BaseModel
from typing import List, Optional, Dict, Set
import os
import glob
import json
from datetime import datetime

from telegram_client import TelegramManager
from downloader import VideoDownloader

from fastapi.responses import JSONResponse
from fastapi.requests import Request

app = FastAPI(title="Downgram Backend API")

# Store active websocket connections
connected_clients: Set[WebSocket] = set()

async def broadcast_downloads():
    if not connected_clients:
        return
    data = list(active_downloads.values())
    message = json.dumps(data)
    disconnected = set()
    for client in list(connected_clients):  # Iterate over copy to avoid modification during iteration
        try:
            await client.send_text(message)
        except Exception:
            disconnected.add(client)
    for client in disconnected:
        connected_clients.discard(client)

@app.websocket("/ws/downloads")
async def websocket_downloads(websocket: WebSocket):
    await websocket.accept()
    connected_clients.add(websocket)
    try:
        # Send initial state
        await websocket.send_text(json.dumps(list(active_downloads.values())))
        # Keep connection alive with periodic pings instead of blocking on receive
        while True:
            try:
                # Use receive with timeout to detect disconnections
                data = await asyncio.wait_for(websocket.receive_text(), timeout=30.0)
                # If client sends 'ping', respond with 'pong'
                if data == "ping":
                    await websocket.send_text("pong")
            except asyncio.TimeoutError:
                # Send a ping to check if connection is alive
                try:
                    await websocket.send_text(json.dumps(list(active_downloads.values())))
                except Exception:
                    break  # Connection is dead, exit loop
            except WebSocketDisconnect:
                break
            except Exception:
                break
    except Exception as e:
        print(f"WebSocket error: {e}")
    finally:
        connected_clients.discard(websocket)

def get_session_dir():
    env_dir = os.environ.get("SESSION_DIR")
    if env_dir:
        return env_dir
    home_dir = os.path.expanduser("~")
    downgram_dir = os.path.join(home_dir, ".downgram")
    if not os.path.exists(downgram_dir):
        os.makedirs(downgram_dir, exist_ok=True)
    return downgram_dir

def get_session_name():
    session_dir = get_session_dir()
    return os.path.join(session_dir, "telegram_session")

telegram_manager: Optional[TelegramManager] = None
downloader = VideoDownloader()

@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    print(f"Global error: {exc}")
    return JSONResponse(
        status_code=500,
        content={"status": "error", "need_code": False, "error": str(exc)}
    )

@app.on_event("shutdown")
async def shutdown_event():
    global telegram_manager
    if telegram_manager:
        await telegram_manager.disconnect()

search_cache: Dict[int, dict] = {}
active_downloads: Dict[int, dict] = {}
# Store download tasks to avoid garbage collection
download_tasks = {}
concurrent_limit = 4
download_semaphore = asyncio.Semaphore(concurrent_limit)

class InitRequest(BaseModel):
    api_id: int
    api_hash: str
    phone: str

class SendCodeRequest(BaseModel):
    api_id: int
    api_hash: str
    phone: str

class VerifyCodeRequest(BaseModel):
    api_id: int
    api_hash: str
    phone: str
    code: str
    password: Optional[str] = None

class SearchRequest(BaseModel):
    entities_ids: List[int]
    keyword: str
    offset: int = 0
    limit: int = 100 # Default limit for "all" search

class DownloadRequest(BaseModel):
    media_id: int
    path: str
    filename: Optional[str] = None

class SetConcurrentRequest(BaseModel):
    max_concurrent: int = 4

@app.post("/set_concurrent")
async def set_concurrent(req: SetConcurrentRequest):
    global concurrent_limit, download_semaphore
    concurrent_limit = max(1, min(16, req.max_concurrent))
    download_semaphore = asyncio.Semaphore(concurrent_limit)
    return {"status": "ok", "max_concurrent": concurrent_limit}

@app.get("/health")
async def health():
    return {"status": "ok"}

@app.get("/download_history")
async def get_download_history():
    path = os.path.join(get_session_dir(), "download_history.json")
    if os.path.exists(path):
        try:
            with open(path, 'r') as f:
                return {"history": json.load(f)}
        except: pass
    return {"history": []}

@app.post("/send_code")
async def send_code(req: SendCodeRequest):
    global telegram_manager
    try:
        if telegram_manager is None:
            telegram_manager = TelegramManager(req.api_id, req.api_hash, req.phone, session_name=get_session_name())
        await telegram_manager.connect()
        if await telegram_manager.is_authorized():
            return {"status": "already_authorized", "need_code": False}
        success = await telegram_manager.send_code()
        return {"status": "code_sent", "need_code": True} if success else {"status": "error", "error": "Failed"}
    except Exception as e:
        return {"status": "error", "error": str(e)}

@app.post("/verify_code")
async def verify_code(req: VerifyCodeRequest):
    global telegram_manager
    try:
        if telegram_manager is None:
            telegram_manager = TelegramManager(req.api_id, req.api_hash, req.phone, session_name=get_session_name())
            await telegram_manager.connect()
        result = await telegram_manager.sign_in(req.code, req.password)
        if result == True:
            # Force a small API call to stabilize the session before saving
            # This ensures all auth data is properly serialized
            try:
                from telethon.tl.functions.users import GetUsersRequest
                from telethon.tl.types import InputUserSelf
                await telegram_manager.client(GetUsersRequest([InputUserSelf()]))
            except Exception as e:
                print(f"[verify_code] Non-critical error during session stabilization: {e}")

            await telegram_manager.save_session()
            return {"status": "authorized", "success": True}
        return {"status": "error", "success": False, "need_password": result == "2fa_needed"}
    except Exception as e:
        return {"status": "error", "error": str(e)}

@app.get("/dialogs")
async def get_dialogs():
    if not telegram_manager: raise HTTPException(status_code=400)
    dialogs = await telegram_manager.get_recent_dialogs()
    return [{"id": d["id"], "title": d["title"], "type": d["type"]} for d in dialogs]

@app.post("/search")
async def search(req: SearchRequest):
    if not telegram_manager: raise HTTPException(status_code=400)
    all_dialogs = await telegram_manager.get_recent_dialogs()
    selected_entities = [d["entity"] for d in all_dialogs if d["id"] in req.entities_ids]
    
    # Increase limit significantly to return "everything" found
    results = await telegram_manager.search_media(selected_entities, req.keyword, req.offset)
    
    serializable_media = []
    for m in results["media"]:
        search_cache[m["id"]] = m
        serializable_media.append({
            "id": m["id"], "date": m["date"], "channel_title": m["channel_title"],
            "message": m["message"], "file_size": m["file_size"], "media_type": m["media_type"],
            "filename": m.get("filename"), "title": m.get("title")
        })
    return {"media": serializable_media, "total_found": results["total_found"], "has_more": results["has_more"]}

@app.post("/download")
async def download_endpoint(req: DownloadRequest):
    if req.media_id not in search_cache:
        raise HTTPException(status_code=404, detail="Media not found")
    
    media_data = search_cache[req.media_id]
    if req.filename:
        media_data["filename"] = req.filename
    task_id = req.media_id
    
    # Create background task and store it
    task = asyncio.create_task(run_download(task_id, media_data, req.path))
    download_tasks[task_id] = task
    
    return {"status": "queued", "id": task_id}

async def run_download(media_id: int, media_data: dict, path: str):
    active_downloads[media_id] = {
        "id": media_id, 
        "filename": media_data.get("filename") or media_data["message"][:30] or f"file_{media_id}", 
        "progress": 0,
        "status": "queued", 
        "size": media_data["file_size"],
        "download_speed": 0,
        "eta": 0
    }
    
    await broadcast_downloads()

    async with download_semaphore:
        active_downloads[media_id]["status"] = "downloading"
        await broadcast_downloads()

        last_broadcast_time = 0
        start_time = time.time()
        last_bytes = 0

        def progress_callback(current, total):
            nonlocal last_broadcast_time, start_time, last_bytes
            if total > 0:
                active_downloads[media_id]["progress"] = int((current / total) * 100)
                
                # Speed calculation
                elapsed = time.time() - start_time
                if elapsed >= 1.0: # Update speed every second
                    bytes_diff = current - last_bytes
                    speed = int(bytes_diff / elapsed)
                    active_downloads[media_id]["download_speed"] = speed
                    
                    # ETA calculation
                    remaining_bytes = total - current
                    if speed > 0:
                        active_downloads[media_id]["eta"] = int(remaining_bytes / speed)
                    else:
                        active_downloads[media_id]["eta"] = 0
                    
                    last_bytes = current
                    start_time = time.time()

                # Throttling broadcasts to avoid flooding (max 5 per second)
                current_time = asyncio.get_event_loop().time()
                if current_time - last_broadcast_time > 0.2:
                    # Use create_task since this callback is not async
                    asyncio.create_task(broadcast_downloads())
                    last_broadcast_time = current_time

        try:
            success = await downloader.download_single_media(
                telegram_manager, 
                media_data,
                custom_path=path,
                progress_callback=progress_callback
            )
            active_downloads[media_id]["status"] = "completed" if success else "failed"
            active_downloads[media_id]["progress"] = 100 if success else 0
            await broadcast_downloads()
        except Exception as e:
            print(f"Download error: {e}")
            active_downloads[media_id]["status"] = "failed"
            await broadcast_downloads()
    # Semaphore released automatically by `async with`
    if media_id in download_tasks:
        del download_tasks[media_id]

@app.get("/downloads")
async def get_downloads():
    return list(active_downloads.values())

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8000)
    args = parser.parse_args()
    uvicorn.run(app, host="127.0.0.1", port=args.port)
