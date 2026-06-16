"""
Módulo de conexión a Telegram usando Telethon (Versión API para Compose)
"""

import asyncio
import os
from typing import List, Dict, Any, Optional
from telethon import TelegramClient
from telethon.errors import FloodWaitError, RPCError, SessionPasswordNeededError
from telethon.sessions import StringSession
from telethon.tl.types import DocumentAttributeFilename

class TelegramManager:
    """Clase para manejar la conexión y operaciones con Telegram"""
    
    def __init__(self, api_id: int, api_hash: str, phone: str, session_name: str = "telegram_session"):
        self.api_id = api_id
        self.api_hash = api_hash
        self.phone = phone
        self.session_name = session_name
        self.client: Optional[TelegramClient] = None
        self.is_connected = False
        self.phone_code_hash = None
    
    async def connect(self):
        """Establece conexión inicial con Telegram"""
        if not self.client:
            # Use StringSession with file persistence to avoid database locks
            session_file = f"{self.session_name}.string"
            session_string = ""
            
            # Load existing session string from file if it exists
            if os.path.exists(session_file):
                try:
                    with open(session_file, 'r') as f:
                        session_string = f.read().strip()
                    print(f"[TelegramManager] Loaded session from {session_file} (length: {len(session_string)})")
                    # Valid Telegram session strings are typically 300+ characters
                    # Empty or very short strings indicate invalid/expired sessions
                    if len(session_string) < 200:
                        print(f"[TelegramManager] Warning: Session string too short ({len(session_string)} chars), treating as invalid")
                        session_string = ""
                        # Delete the invalid session file
                        try:
                            os.remove(session_file)
                            print(f"[TelegramManager] Deleted invalid session file: {session_file}")
                        except Exception as e:
                            print(f"[TelegramManager] Error deleting invalid session file: {e}")
                    else:
                        print(f"[TelegramManager] Session string looks valid ({len(session_string)} chars)")
                except Exception as e:
                    print(f"[TelegramManager] Warning: Could not load session file: {e}")
                    session_string = ""
            else:
                print(f"[TelegramManager] No existing session file found: {session_file}")
            
            # Create StringSession (empty string if no existing session)
            session = StringSession(session_string)
            self.client = TelegramClient(session, self.api_id, self.api_hash)
            
            # Save session string after connection
            self._session_file = session_file
        
        if not self.client.is_connected():
            await self.client.connect()
            self.is_connected = True

    async def is_authorized(self) -> bool:
        """Verifica si el usuario está autorizado"""
        if not self.client:
            return False
        return await self.client.is_user_authorized()

    async def save_session(self):
        """Save the current session to file"""
        if not self.client or not self._session_file:
            print(f"[TelegramManager] Cannot save session: client={self.client is not None}, session_file={self._session_file}")
            return
        try:
            # Ensure client is connected before saving
            if not self.client.is_connected():
                print(f"[TelegramManager] Reconnecting before saving session...")
                await self.client.connect()

            session_string = self.client.session.save()
            print(f"[TelegramManager] Generated session string (length: {len(session_string)})")

            # Validate session string looks reasonable
            if len(session_string) < 200:
                print(f"[TelegramManager] Warning: Generated session string is suspiciously short ({len(session_string)} chars)")
                # Try to get more auth info by making a simple API call
                try:
                    from telethon.tl.functions.users import GetUsersRequest
                    from telethon.tl.types import InputUserSelf
                    await self.client(GetUsersRequest([InputUserSelf()]))
                    session_string = self.client.session.save()
                    print(f"[TelegramManager] Re-generated session string after API call (length: {len(session_string)})")
                except Exception as api_e:
                    print(f"[TelegramManager] Could not refresh session: {api_e}")

            with open(self._session_file, 'w') as f:
                f.write(session_string)
            print(f"[TelegramManager] Saved session to {self._session_file} (length: {len(session_string)})")
        except Exception as e:
            print(f"[TelegramManager] Warning: Could not save session file: {e}")
            import traceback
            traceback.print_exc()

    async def send_code(self):
        """Envía el código de verificación al teléfono"""
        try:
            print(f"[TelegramManager] Sending code request to phone: {self.phone}")
            result = await self.client.send_code_request(self.phone)
            self.phone_code_hash = result.phone_code_hash
            print(f"[TelegramManager] Code sent successfully, phone_code_hash: {self.phone_code_hash[:10]}...")
            return True
        except Exception as e:
            print(f"[TelegramManager] Error sending code: {type(e).__name__}: {e}")
            import traceback
            traceback.print_exc()
            return False

    async def sign_in(self, code: str, password: str = None):
        """Inicia sesión con el código (y contraseña si es necesario)"""
        try:
            if password:
                await self.client.sign_in(password=password)
            else:
                await self.client.sign_in(self.phone, code, phone_code_hash=self.phone_code_hash)
            return True
        except SessionPasswordNeededError:
            return "2fa_needed"
        except Exception as e:
            print(f"Error signing in: {e}")
            return False

    async def get_recent_dialogs(self, limit: int = 50) -> List[Dict[str, Any]]:
        """Obtiene los diálogos recientes"""
        if not self.is_connected or not self.client:
            print("[TelegramManager] Not connected, cannot get dialogs")
            return []
        
        # Check authorization
        try:
            is_authorized = await self.is_authorized()
            if not is_authorized:
                print("[TelegramManager] User not authorized, cannot get dialogs")
                return []
        except Exception as e:
            print(f"[TelegramManager] Error checking authorization: {e}")
            return []
        
        try:
            print(f"[TelegramManager] Fetching dialogs from Telegram...")
            dialogs = []
            count = 0
            
            # Add retry logic for database lock errors during iteration
            max_retries = 5
            for attempt in range(max_retries):
                try:
                    async for dialog in self.client.iter_dialogs(limit=limit):
                        count += 1
                        entity = dialog.entity
                        is_bot_chat = dialog.is_user and hasattr(entity, 'bot') and entity.bot
                        
                        if dialog.is_channel or dialog.is_group or is_bot_chat:
                            dialog_type = 'Bot' if is_bot_chat else ('Canal' if dialog.is_channel else 'Grupo')
                            title = dialog.title or getattr(entity, 'first_name', 'Sin nombre')
                            print(f"[TelegramManager] Found dialog: {title} (type: {dialog_type}, id: {dialog.id})")
                            dialogs.append({
                                'id': dialog.id,
                                'title': title,
                                'type': dialog_type,
                                'entity': entity
                            })
                    print(f"[TelegramManager] Total dialogs found: {len(dialogs)} out of {count} checked")
                    return dialogs
                except Exception as e:
                    if "database is locked" in str(e) and attempt < max_retries - 1:
                        delay = 3 + attempt  # Increasing delay: 3, 4, 5, 6, 7 seconds
                        print(f"[TelegramManager] Database locked during dialog fetch, retrying in {delay} seconds... (attempt {attempt + 1}/{max_retries})")
                        await asyncio.sleep(delay)
                    else:
                        raise
            
            return dialogs
        except Exception as e:
            print(f"[TelegramManager] Error getting dialogs: {e}")
            import traceback
            traceback.print_exc()
            return []
    
    async def search_media(self, entities: List[Any], keyword: str, offset: int = 0) -> Dict[str, Any]:
        """Busca videos y audios"""
        media_files = []
        page_size = 50
        
        for entity in entities:
            try:
                search_param = keyword if keyword else None
                msg_limit = 100 if not keyword else None
                async for message in self.client.iter_messages(entity=entity, search=search_param, limit=msg_limit):
                    media_type = None
                    media_info = None
                    
                    if message.video:
                        media_type, media_info = 'video', message.video
                    elif message.audio:
                        media_type, media_info = 'audio', message.audio
                    
                    if media_info:
                        # Extraer nombre de archivo si existe
                        filename = None
                        title = None
                        if hasattr(media_info, 'attributes'):
                            for attr in media_info.attributes:
                                if isinstance(attr, DocumentAttributeFilename):
                                    filename = attr.file_name
                                if media_type == 'audio':
                                    if hasattr(attr, 'title') and attr.title:
                                        title = attr.title
                                    if hasattr(attr, 'performer') and attr.performer and title:
                                        title = f"{attr.performer} - {title}"
                                    elif hasattr(attr, 'performer') and attr.performer:
                                        title = attr.performer
                        
                        media_files.append({
                            'id': message.id,
                            'date': message.date.strftime('%Y-%m-%d %H:%M'),
                            'channel_title': getattr(entity, 'title', 'Desconocido'),
                            'message': message.message or 'Sin descripción',
                            'file_size': getattr(media_info, 'size', getattr(media_info, 'file_size', 0)),
                            'media_type': media_type,
                            'filename': filename,
                            'title': title,
                            'message_obj': message,
                            'entity': entity
                        })
            except Exception:
                continue
        
        media_files.sort(key=lambda x: x['date'], reverse=True)
        paginated = media_files[offset:offset + page_size]
        
        return {
            'media': paginated,
            'total_found': len(media_files),
            'has_more': (offset + page_size) < len(media_files)
        }

    async def disconnect(self):
        if self.client:
            await self.client.disconnect()
            self.is_connected = False

    async def download_media(self, message, entity, file_path: str, progress_callback=None):
        """Descarga un archivo de medios desde un mensaje"""
        if not self.client or not self.is_connected:
            print("[TelegramManager] Cannot download: client not connected")
            return False
        
        try:
            print(f"[TelegramManager] Starting download to: {file_path}")
            # Descargar el archivo
            await self.client.download_media(
                message=message,
                file=file_path,
                progress_callback=progress_callback
            )
            print(f"[TelegramManager] Download completed: {file_path}")
            return True
        except FloodWaitError as e:
            print(f"FloodWaitError: esperando {e.seconds} segundos")
            await asyncio.sleep(e.seconds)
            # Reintentar una vez
            try:
                await self.client.download_media(
                    message=message,
                    file=file_path,
                    progress_callback=progress_callback
                )
                return True
            except Exception as e:
                print(f"[TelegramManager] Error on retry download: {e}")
                return False
        except Exception as e:
            print(f"Error downloading media: {e}")
            import traceback
            traceback.print_exc()
            return False
