from typing import Any, Dict, List, Optional
from core.firebase import fb

class FirestoreDB:
    def __init__(self, collection_name: str):
        self.collection_name = collection_name

    @property
    def collection(self):
        if fb.db:
            return fb.db.collection(self.collection_name)
        return None

    def add(self, data: Dict[str, Any], document_id: Optional[str] = None) -> Optional[str]:
        if not self.collection:
            return None
        
        if document_id:
            self.collection.document(document_id).set(data)
            return document_id
        else:
            _, doc_ref = self.collection.add(data)
            return doc_ref.id

    def get(self, document_id: str) -> Optional[Dict[str, Any]]:
        if not self.collection:
            return None
        doc = self.collection.document(document_id).get()
        return doc.to_dict() if doc.exists else None

    def list(self, limit: int = 100, order_by: str = "timestamp", descending: bool = True) -> List[Dict[str, Any]]:
        if not self.collection:
            return []
        
        query = self.collection.order_by(order_by, direction=firestore.Query.DESCENDING if descending else firestore.Query.ASCENDING)
        docs = query.limit(limit).stream()
        return [doc.to_dict() for doc in docs]

from firebase_admin import firestore # for Query constants
