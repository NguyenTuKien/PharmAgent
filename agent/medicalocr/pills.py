import json
import sqlite3
import uuid
from typing import Any, Dict, List, Optional
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from .database import DEFAULT_DB_PATH
from .normalize import normalize_name

router = APIRouter()

class PillImageBase(BaseModel):
    imageUrl: str
    viewType: Optional[str] = "OTHER"
    isPrimary: Optional[bool] = False

class PillImageResponse(PillImageBase):
    id: str

class PillCreateRequest(BaseModel):
    name: str
    activeIngredient: Optional[str] = ""
    dosage: Optional[str] = ""
    manufacturer: Optional[str] = ""

class PillRequest(PillCreateRequest):
    pass

class PillResponse(PillCreateRequest):
    id: str
    images: List[PillImageResponse] = []

class PillCatalogResponse(BaseModel):
    id: str
    name: str
    activeIngredient: Optional[str] = ""
    dosage: Optional[str] = ""
    manufacturer: Optional[str] = ""
    sourceUrl: Optional[str] = ""
    source_url: Optional[str] = ""
    images: List[PillImageResponse] = []
    imageUrls: List[str] = []

class PagePillCatalogResponse(BaseModel):
    content: List[PillCatalogResponse]
    totalPages: int
    totalElements: int

def init_pills_db():
    conn = sqlite3.connect(DEFAULT_DB_PATH)
    try:
        conn.execute("ALTER TABLE products ADD COLUMN images TEXT;")
    except sqlite3.OperationalError:
        pass # Column might already exist
    conn.commit()
    conn.close()

def _parse_images(images_str: Optional[str]) -> List[Dict[str, Any]]:
    if not images_str:
        return []
    try:
        return json.loads(images_str)
    except Exception:
        return []

def _build_pill_response(row: sqlite3.Row) -> Dict[str, Any]:
    images = _parse_images(row["images"]) if "images" in row.keys() else []
    # If primary_image_url exists and images is empty, we can mock it
    primary_image_url = row["primary_image_url"]
    if primary_image_url and not images:
        images = [{"id": "default", "imageUrl": primary_image_url, "viewType": "FRONT", "isPrimary": True}]

    # Try to extract the id from source_url (local://pill/{id}) or use stringified SQLite id
    source_url = row["source_url"] or ""
    if source_url.startswith("local://pill/"):
        item_id = source_url.replace("local://pill/", "")
    else:
        item_id = str(row["id"])

    return {
        "id": item_id,
        "name": row["display_name"],
        "activeIngredient": row["active_ingredient"],
        "dosage": row["normalized_dosage"], # Frontend dosage maps to normalized_dosage
        "manufacturer": row["manufacturer_name"],
        "sourceUrl": source_url,
        "source_url": source_url,
        "images": images,
        "imageUrls": [img.get("imageUrl") for img in images if img.get("imageUrl")]
    }

@router.get("/pills", response_model=PagePillCatalogResponse)
def get_pill_catalog(search: Optional[str] = "", page: int = 0, size: int = 10):
    init_pills_db()
    conn = sqlite3.connect(DEFAULT_DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()

    query = "SELECT * FROM products"
    params = []
    
    if search:
        search = search.strip()
        query += " WHERE display_name LIKE ? OR active_ingredient LIKE ? OR manufacturer_name LIKE ?"
        like_search = f"%{search}%"
        params.extend([like_search, like_search, like_search])
    
    query += " ORDER BY id DESC"
    
    cursor.execute(query, params)
    all_rows = cursor.fetchall()
    conn.close()

    total_elements = len(all_rows)
    total_pages = (total_elements + size - 1) // size

    start = page * size
    end = start + size
    page_rows = all_rows[start:end]

    content = [_build_pill_response(row) for row in page_rows]

    return {
        "content": content,
        "totalPages": total_pages,
        "totalElements": total_elements
    }

@router.get("/pills/search", response_model=List[PillCatalogResponse])
def search_pills(keyword: Optional[str] = "", limit: int = 10):
    page = get_pill_catalog(search=keyword or "", page=0, size=max(1, min(limit, 25)))
    return page["content"]

@router.get("/pills/{pill_id}")
def get_pill_by_id(pill_id: str):
    init_pills_db()
    conn = sqlite3.connect(DEFAULT_DB_PATH)
    conn.row_factory = sqlite3.Row
    source_url = f"local://pill/{pill_id}"
    row = conn.execute("SELECT * FROM products WHERE source_url = ?", (source_url,)).fetchone()
    if not row:
        row = conn.execute("SELECT * FROM products WHERE source_url = ?", (pill_id,)).fetchone()
    if not row:
        # Fallback to id
        row = conn.execute("SELECT * FROM products WHERE id = ?", (pill_id,)).fetchone()
    conn.close()
    
    if not row:
        raise HTTPException(status_code=404, detail="Pill not found")
        
    return _build_pill_response(row)

@router.post("/admin/pills", response_model=PillResponse)
def create_pill(request: PillCreateRequest):
    from main import reload_catalog # Lazy import to avoid circular dependency
    init_pills_db()
    
    pill_id = str(uuid.uuid4())
    source_url = f"local://pill/{pill_id}"
    
    conn = sqlite3.connect(DEFAULT_DB_PATH)
    try:
        conn.execute(
            """
            INSERT INTO products (
                source_url, display_name, normalized_name, normalized_dosage,
                brand_name, manufacturer_name, active_ingredient, primary_image_url, images
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                source_url,
                request.name,
                normalize_name(request.name),
                request.dosage,
                request.manufacturer,
                request.manufacturer,
                request.activeIngredient,
                "", # No primary image initially
                "[]"
            )
        )
        conn.commit()
    finally:
        conn.close()
        
    reload_catalog()
    return get_pill_by_id(pill_id)

@router.put("/admin/pills/{pill_id}", response_model=PillResponse)
def update_pill(pill_id: str, request: PillRequest):
    from main import reload_catalog
    init_pills_db()
    
    conn = sqlite3.connect(DEFAULT_DB_PATH)
    source_url = f"local://pill/{pill_id}"
    row = conn.execute("SELECT id FROM products WHERE source_url = ?", (source_url,)).fetchone()
    
    if not row:
        # Fallback to integer id
        row = conn.execute("SELECT id FROM products WHERE id = ?", (pill_id,)).fetchone()
        
    if not row:
        conn.close()
        raise HTTPException(status_code=404, detail="Pill not found")
        
    db_id = row[0]
    
    conn.execute(
        """
        UPDATE products SET
            display_name = ?,
            normalized_name = ?,
            normalized_dosage = ?,
            brand_name = ?,
            manufacturer_name = ?,
            active_ingredient = ?
        WHERE id = ?
        """,
        (
            request.name,
            normalize_name(request.name),
            request.dosage,
            request.manufacturer,
            request.manufacturer,
            request.activeIngredient,
            db_id
        )
    )
    conn.commit()
    conn.close()
    
    reload_catalog()
    return get_pill_by_id(pill_id)

@router.delete("/admin/pills/{pill_id}")
def delete_pill(pill_id: str):
    from main import reload_catalog
    init_pills_db()
    
    conn = sqlite3.connect(DEFAULT_DB_PATH)
    source_url = f"local://pill/{pill_id}"
    conn.execute("DELETE FROM products WHERE source_url = ? OR id = ?", (source_url, pill_id))
    conn.commit()
    conn.close()
    
    reload_catalog()
    return {"message": "Success"}

@router.post("/admin/pills/{pill_id}/images", response_model=PillResponse)
def add_pill_image(pill_id: str, request: PillImageBase):
    from main import reload_catalog
    init_pills_db()
    
    conn = sqlite3.connect(DEFAULT_DB_PATH)
    conn.row_factory = sqlite3.Row
    source_url = f"local://pill/{pill_id}"
    
    row = conn.execute("SELECT id, primary_image_url, images FROM products WHERE source_url = ? OR id = ?", (source_url, pill_id)).fetchone()
    if not row:
        conn.close()
        raise HTTPException(status_code=404, detail="Pill not found")
        
    db_id = row["id"]
    images = _parse_images(row["images"]) if "images" in row.keys() else []
    primary_image_url = row["primary_image_url"]
    
    new_image = {
        "id": str(uuid.uuid4()),
        "imageUrl": request.imageUrl,
        "viewType": request.viewType,
        "isPrimary": request.isPrimary
    }
    
    if request.isPrimary or not images:
        primary_image_url = request.imageUrl
        for img in images:
            img["isPrimary"] = False
        new_image["isPrimary"] = True
        
    images.append(new_image)
    
    conn.execute(
        "UPDATE products SET images = ?, primary_image_url = ? WHERE id = ?",
        (json.dumps(images), primary_image_url, db_id)
    )
    conn.commit()
    conn.close()
    
    reload_catalog()
    return get_pill_by_id(pill_id)

@router.delete("/admin/pills/{pill_id}/images/{image_id}")
def delete_pill_image(pill_id: str, image_id: str):
    from main import reload_catalog
    init_pills_db()
    
    conn = sqlite3.connect(DEFAULT_DB_PATH)
    conn.row_factory = sqlite3.Row
    source_url = f"local://pill/{pill_id}"
    
    row = conn.execute("SELECT id, primary_image_url, images FROM products WHERE source_url = ? OR id = ?", (source_url, pill_id)).fetchone()
    if not row:
        conn.close()
        raise HTTPException(status_code=404, detail="Pill not found")
        
    db_id = row["id"]
    images = _parse_images(row["images"]) if "images" in row.keys() else []
    
    filtered_images = [img for img in images if img.get("id") != image_id]
    
    if len(filtered_images) == len(images):
        conn.close()
        raise HTTPException(status_code=404, detail="Image not found")
        
    # Re-evaluate primary_image_url
    primary_image_url = row["primary_image_url"]
    if not any(img.get("isPrimary") for img in filtered_images):
        if filtered_images:
            filtered_images[0]["isPrimary"] = True
            primary_image_url = filtered_images[0]["imageUrl"]
        else:
            primary_image_url = ""
            
    conn.execute(
        "UPDATE products SET images = ?, primary_image_url = ? WHERE id = ?",
        (json.dumps(filtered_images), primary_image_url, db_id)
    )
    conn.commit()
    conn.close()
    
    reload_catalog()
    return {"message": "Success"}
