from sqlalchemy.ext.asyncio import AsyncSession


class BaseService:
    """服务基类"""

    def __init__(self, db: AsyncSession):
        self.db = db
