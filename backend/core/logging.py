import logging
from logging.config import dictConfig
from backend.config.settings import settings


LOGGING_CONFIG = {
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        "standard": {
            "format": "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
        },
        "detailed": {
            "format": "%(asctime)s - %(name)s - %(levelname)s - %(module)s:%(lineno)d - %(message)s"
        }
    },
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
            "formatter": "standard",
            "level": settings.LOG_LEVEL
        },
        "file": {
            "class": "logging.FileHandler",
            "formatter": "detailed",
            "level": settings.LOG_LEVEL,
            "filename": "app.log"
        }
    },
    "loggers": {
        "": {
            "handlers": ["console", "file"],
            "level": settings.LOG_LEVEL,
            "propagate": True
        },
        "uvicorn": {
            "handlers": ["console"],
            "level": "INFO",
            "propagate": False
        },
        "sqlalchemy": {
            "handlers": ["console"],
            "level": "WARNING",
            "propagate": False
        }
    }
}


# 配置日志
dictConfig(LOGGING_CONFIG)

# 创建应用日志记录器
logger = logging.getLogger("safewalk")
