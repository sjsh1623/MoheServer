"""
FastAPI Embedding Server using kanana-nano-2.1b-embedding
OpenAI API compatible endpoints
"""
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Union
from contextlib import asynccontextmanager
import torch
from transformers import AutoTokenizer, AutoModel
import uvicorn
import logging
import os

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Global model and tokenizer
model = None
tokenizer = None
device = None

class EmbeddingRequest(BaseModel):
    input: Union[str, List[str]]
    model: str = "kanana-nano-2.1b-embedding"
    instruction: str = ""

class EmbeddingResponse(BaseModel):
    object: str = "list"
    data: List[dict]
    model: str
    usage: dict

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Load the embedding model on startup"""
    global model, tokenizer, device

    logger.info("Loading kanana-nano-2.1b-embedding model...")

    # Determine device
    if torch.cuda.is_available():
        device = torch.device("cuda")
        logger.info("Using CUDA")
    elif torch.backends.mps.is_available():
        device = torch.device("mps")
        logger.info("Using MPS (Apple Silicon)")
    else:
        device = torch.device("cpu")
        logger.info("Using CPU")

    model_name = "kakaocorp/kanana-nano-2.1b-embedding"

    try:
        # Try to import bitsandbytes for quantization support
        try:
            import bitsandbytes
            has_bitsandbytes = True
        except ImportError:
            has_bitsandbytes = False
            logger.info("bitsandbytes not available - will use memory-efficient loading")

        # Check if CUDA is available and bitsandbytes is installed for 8-bit quantization
        use_bitsandbytes = torch.cuda.is_available() and has_bitsandbytes

        if use_bitsandbytes:
            # Load model with 8-bit quantization to reduce memory usage (CUDA only)
            # This reduces memory from ~6GB to ~2-3GB
            logger.info("Loading model with 8-bit quantization (CUDA available)")
            model = AutoModel.from_pretrained(
                model_name,
                trust_remote_code=True,
                load_in_8bit=True,  # Enable 8-bit quantization
                device_map="auto"    # Automatically handle device placement
            )
            quantization_status = "with 8-bit bitsandbytes quantization"
        else:
            # Load model for CPU/MPS with memory optimizations
            logger.info("Loading model with memory-efficient settings")

            # Use low_cpu_mem_usage to reduce peak memory during loading
            # This loads the model more gradually
            model = AutoModel.from_pretrained(
                model_name,
                trust_remote_code=True,
                torch_dtype=torch.float32,
                low_cpu_mem_usage=True  # Reduce peak memory during loading
            )

            # Move to device after loading
            model = model.to(device)

            # Enable memory-efficient attention if available
            try:
                if hasattr(model, "enable_mem_efficient_attention"):
                    model.enable_mem_efficient_attention()
                    logger.info("Memory-efficient attention enabled")
            except Exception as mem_err:
                logger.warning(f"Could not enable memory-efficient attention: {mem_err}")

            quantization_status = "with memory-efficient loading (float32)"

        tokenizer = AutoTokenizer.from_pretrained(
            model_name,
            trust_remote_code=True
        )

        model.eval()
        logger.info(f"Model loaded successfully {quantization_status} on {device}")

    except Exception as e:
        logger.error(f"Failed to load model: {e}")
        raise

    yield  # Server is running

    # Cleanup (if needed)
    logger.info("Shutting down embedding server")

app = FastAPI(title="Kanana Embedding Server", version="1.0.0", lifespan=lifespan)

@app.get("/")
async def root():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "model": "kanana-nano-2.1b-embedding",
        "device": str(device)
    }

@app.get("/health")
async def health():
    """Health check endpoint"""
    return {"status": "healthy"}

@app.post("/v1/embeddings", response_model=EmbeddingResponse)
async def create_embeddings(request: EmbeddingRequest):
    """
    Create embeddings for input text(s)
    OpenAI API compatible endpoint
    """
    if model is None or tokenizer is None:
        raise HTTPException(status_code=503, detail="Model not loaded")

    # Normalize input to list
    texts = [request.input] if isinstance(request.input, str) else request.input

    try:
        # Get embeddings using the model's encode method
        with torch.no_grad():
            embeddings = model.encode(
                texts,
                instruction=request.instruction,
                max_length=512
            )

            # Convert to list of floats
            if isinstance(embeddings, torch.Tensor):
                embeddings = embeddings.cpu().float().numpy()

        # Format response in OpenAI style
        data = [
            {
                "object": "embedding",
                "embedding": emb.tolist(),
                "index": idx
            }
            for idx, emb in enumerate(embeddings)
        ]

        total_tokens = sum(len(tokenizer.encode(text)) for text in texts)

        return EmbeddingResponse(
            object="list",
            data=data,
            model=request.model,
            usage={
                "prompt_tokens": total_tokens,
                "total_tokens": total_tokens
            }
        )

    except Exception as e:
        logger.error(f"Error generating embeddings: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/embeddings")
async def create_embeddings_simple(request: EmbeddingRequest):
    """Simple embedding endpoint (non-OpenAI format)"""
    return await create_embeddings(request)

if __name__ == "__main__":
    port = int(os.getenv("PORT", "8000"))
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=port,
        log_level="info"
    )
