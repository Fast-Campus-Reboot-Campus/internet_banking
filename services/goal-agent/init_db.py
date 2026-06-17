import sys
import os
sys.path.insert(0, '.')
os.environ['DATABASE_URL'] = 'postgresql+psycopg://teamproject:teamproject@127.0.0.1:5432/teamproject'
from sqlalchemy import text
from app.database import Base, engine
from app import models
with engine.begin() as conn:
    conn.execute(text('DROP SCHEMA public CASCADE'))
    conn.execute(text('CREATE SCHEMA public'))
    conn.execute(text('GRANT ALL ON SCHEMA public TO teamproject'))
Base.metadata.create_all(engine)
print('완료')
