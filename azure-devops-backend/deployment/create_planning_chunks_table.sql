-- Habilitar la extensión pgvector si aún no está activa en PostgreSQL
CREATE EXTENSION IF NOT EXISTS vector;

-- Eliminar la tabla si existe para evitar conflictos en reinicios
DROP TABLE IF EXISTS planning_chunks;

-- Crear la tabla planning_chunks para almacenar los fragmentos de Markdown vectorizados
CREATE TABLE planning_chunks
(
    id        UUID PRIMARY KEY,
    content   TEXT  NOT NULL, -- Fragmento de texto Markdown
    metadata  JSONB NOT NULL, -- Metadatos de control (initiative_id, section_name, etc.)
    embedding vector(1536)    -- Vector de embeddings (1536 dimensiones para text-embedding-ada-002)
);

-- Crear un índice HNSW para búsquedas vectoriales rápidas por similitud de coseno
CREATE INDEX ON planning_chunks USING hnsw (embedding vector_cosine_ops);
