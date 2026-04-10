import firebase_admin
from firebase_admin import credentials, firestore
from core.config import settings
import logging

logger = logging.getLogger(__name__)

class FirebaseManager:
    def __init__(self):
        self._db = None
        self._initialized = False

    def initialize(self):
        if self._initialized:
            return

        try:
            if settings.firebase_service_account_path:
                cred = credentials.Certificate(settings.firebase_service_account_path)
                firebase_admin.initialize_app(cred)
            else:
                # Try to initialize with default credentials (useful for production or if environment variables are set)
                firebase_admin.initialize_app()
            
            self._db = firestore.client()
            self._initialized = True
            logger.info("Firebase initialized successfully")
        except Exception as e:
            logger.error(f"Failed to initialize Firebase: {e}")
            # We don't raise error here to allow the app to start even without Firebase, 
            # but specific features will fail.

    @property
    def db(self):
        if not self._initialized:
            self.initialize()
        return self._db

fb = FirebaseManager()
