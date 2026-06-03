from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """应用配置类"""

    model_config = SettingsConfigDict(
        env_file="backend/.env",
        case_sensitive=True,
    )

    # 应用基本配置
    APP_NAME: str = "SafeWalk Backend"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = False

    # 数据库配置
    DATABASE_URL: str = "postgresql+asyncpg://postgres:password@db.project-ref.supabase.co:5432/postgres"
    DATABASE_POOL_SIZE: int = 10
    DATABASE_MAX_OVERFLOW: int = 10
    DATABASE_POOL_TIMEOUT_SECONDS: int = 30

    # Clerk认证配置
    CLERK_SECRET_KEY: str = ""
    CLERK_PUBLISHABLE_KEY: str = ""
    CLERK_JWT_KEY: str = ""
    CLERK_AUTHORIZED_PARTIES: list[str] = []

    # 服务器配置
    HOST: str = "0.0.0.0"
    PORT: int = 8000

    # CORS配置
    BACKEND_CORS_ORIGINS: list[str] = ["*"]

    # 日志配置
    LOG_LEVEL: str = "INFO"

    # 千问配置
    QWEN_API_KEY: str = ""
    QWEN_BASE_URL: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    QWEN_MODEL: str = "qwen-plus"

    # 行程监控配置
    DEVIATION_THRESHOLD_METERS: float = 150.0
    TIMEOUT_GRACE_MINUTES: int = 10


settings = Settings()
