"""
Módulo de descarga de videos (Versión API para Compose)
"""

import os
from typing import List, Dict, Any, Optional
from pathlib import Path
import re

class VideoDownloader:
    """Clase para manejar la descarga de videos"""
    
    def __init__(self, downloads_folder: str = None):
        if downloads_folder is None:
            self.downloads_folder = Path.home() / "Downloads" / "Downgram"
        else:
            self.downloads_folder = Path(downloads_folder)

    def sanitize_filename(self, filename: str) -> str:
        invalid_chars = r'[<>:"/\\|?*]'
        filename = re.sub(invalid_chars, '_', filename)
        return filename[:100]
    
    async def download_single_media(self, telegram_manager, media: Dict[str, Any], custom_path: str = None, progress_callback=None) -> bool:
        try:
            folder = Path(custom_path) if custom_path else self.downloads_folder
            folder.mkdir(parents=True, exist_ok=True)
            
            # Generar nombre
            ext = 'mp4' if media.get('media_type') == 'video' else 'mp3'
            
            # Intentar obtener el nombre del archivo original si existe
            filename = media.get('filename')
            if not filename:
                # Usar el mensaje/título como fallback
                title = media.get('title') or media.get('message') or f"file_{media['id']}"
                filename = f"{self.sanitize_filename(title)}.{ext}"
            
            file_path = folder / filename
            
            print(f"[VideoDownloader] Downloading to: {file_path}")
            
            return await telegram_manager.download_media(
                message=media['message_obj'],
                entity=media['entity'],
                file_path=str(file_path),
                progress_callback=progress_callback
            )
        except Exception as e:
            print(f"[VideoDownloader] Error in download_single_media: {e}")
            import traceback
            traceback.print_exc()
            return False
