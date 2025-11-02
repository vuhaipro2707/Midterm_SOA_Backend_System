from fastapi import FastAPI, Request, HTTPException
from fastapi.responses import JSONResponse
import httpx
from jose import jwt, JWTError
import os
from json.decoder import JSONDecodeError
import yaml

from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(title="API Gateway",
              openapi_url=None)

origins = [
    "http://127.0.0.1:5500",
    "http://localhost:5500",
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

PUBLIC_KEY_PATH = os.path.join(os.path.dirname(__file__), "public_key.pem")
ALGORITHM = "RS256"

SYSTEM_SERVICES = {
    "auth": "http://auth-service:8081", 
    "customer": "http://customer-management-service:8082",
    "payment": "http://payment-processor-service:8083",
    "tuition": "http://tuition-service:8084",
    "otp": "http://otp-service:8085",
}

client = httpx.AsyncClient(timeout=None)

def get_public_key():
    try:
        with open(PUBLIC_KEY_PATH, "r") as f:
            public_key_pem = f.read()
            return public_key_pem
    except FileNotFoundError:
        raise RuntimeError("Public Key file not found!")
    except Exception as e:
        raise RuntimeError(f"Error reading Public Key: {e}")

try:
    PUBLIC_KEY = get_public_key()
except RuntimeError as e:
    print(f"FATAL ERROR: {e}")

def decode_jwt_token(token: str):
    try:
        payload = jwt.decode(
            token, 
            PUBLIC_KEY,
            algorithms=[ALGORITHM]
        )
        return payload
    except JWTError as e:
        raise HTTPException(status_code=401, detail=f"Invalid authentication credentials: {e}")
    

# --- Thêm endpoint để phục vụ tài liệu OpenAPI tùy chỉnh ---
@app.get("/openapi.json", include_in_schema=False)
async def get_openapi_json():
    OPENAPI_FILE_PATH = os.path.join(os.path.dirname(__file__), "openapi.yaml")
    CUSTOM_OPENAPI_SPEC = None
    
    class NoDatesSafeLoader(yaml.SafeLoader):
        @staticmethod
        def remove_timestamp_resolver():
            if 'tag:yaml.org,2002:timestamp' in NoDatesSafeLoader.yaml_resolvers:
                NoDatesSafeLoader.yaml_resolvers.pop('tag:yaml.org,2002:timestamp')
    
    try:
        if 'tag:yaml.org,2002:timestamp' in yaml.SafeLoader.yaml_implicit_resolvers:
            del yaml.SafeLoader.yaml_implicit_resolvers['tag:yaml.org,2002:timestamp']
        
        def str_presenter(dumper, data):
            return dumper.represent_scalar('tag:yaml.org,2002:str', data)

        def str_constructor(loader, node):
            return loader.construct_scalar(node)

        yaml.add_constructor('tag:yaml.org,2002:timestamp', str_constructor, Loader=yaml.SafeLoader)
        yaml.add_constructor('tag:yaml.org,2002:float', str_constructor, Loader=yaml.SafeLoader)
    except Exception as e:
        print(f"INFO: Could not modify yaml.SafeLoader: {e}")

    try:
        with open(OPENAPI_FILE_PATH, "r", encoding="utf-8") as f:
            print(f"Loading OpenAPI specification from {OPENAPI_FILE_PATH}")
            yaml_content = f.read()
            CUSTOM_OPENAPI_SPEC = yaml.safe_load(yaml_content)
            
    except FileNotFoundError:
        print(f"FATAL ERROR: OpenAPI specification file not found at {OPENAPI_FILE_PATH}")
    except Exception as e:
        print(f"FATAL ERROR: Error loading OpenAPI specification: {e}")
    
    if CUSTOM_OPENAPI_SPEC is None:
         raise HTTPException(status_code=500, detail="OpenAPI specification is not available.")
         
    return JSONResponse(CUSTOM_OPENAPI_SPEC)

# Sử dụng Swagger UI tùy chỉnh
@app.get("/docs", include_in_schema=False)
async def custom_swagger_ui_html():
    from fastapi.openapi.docs import get_swagger_ui_html
    return get_swagger_ui_html(
        openapi_url="/openapi.json",
        title=app.title + " - Swagger UI"
    )
# --------------------------------------------------------


@app.api_route("/{path:path}", methods=["GET", "POST", "PUT", "DELETE", "PATCH"])
async def proxy_requests(path: str, request: Request):
    try:
        service_name, sub_path = path.split("/", 1)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid path format. Expecting /<service_name>/<path>")
    
    if service_name not in SYSTEM_SERVICES:
        raise HTTPException(status_code=404, detail=f"Service '{service_name}' not found.")
        
    base_url = SYSTEM_SERVICES[service_name]
    
    headers = dict(request.headers)

    if "content-length" in headers:
        del headers["content-length"]
    if "transfer-encoding" in headers:
        del headers["transfer-encoding"]

    is_public_path = (service_name == "auth" and (sub_path.startswith("login") or sub_path.startswith("logout")))

    if not is_public_path:
        token = request.cookies.get("jwt_token") 
        
        if not token:
            raise HTTPException(status_code=401, detail="Authentication required. JWT Cookie not found.")

        try:
            token_payload = decode_jwt_token(token)
        except HTTPException as e:
            response = JSONResponse(
                content={"detail": "Authentication token expired or invalid."},
                status_code=401
            )
            response.delete_cookie(key="jwt_token", path="/", httponly=True)
            return response
        
        customer_id = token_payload.get("customerId")

        headers["X-Customer-Id"] = str(customer_id)

    try:
        json_data = await request.json()
    except:
        json_data = None
        
    target_url = f"{base_url}/{sub_path}"
    
    try:
        response = await client.request(
            method=request.method,
            url=target_url,
            headers=headers,
            json=json_data,
            params=request.query_params
        )

        response_content = None

        try:
            response_content = response.json()
            
        except JSONDecodeError as e:

            raise HTTPException(
                status_code=502, # 502 Bad Gateway: Lỗi từ Server trung gian (Backend)
                detail={
                    "error": f"Backend service '{service_name}' returned non-JSON data.",
                    "status_code": response.status_code,
                    "raw_response": response.text[:200] # Giới hạn 200 ký tự để tránh payload lớn
                }
            )
        
        except Exception as e:
            raise HTTPException(
                status_code=500,
                detail=f"An unexpected error occurred while processing response from '{service_name}': {e}"
            )

        return JSONResponse(
            content=response_content,
            status_code=response.status_code,
            headers=dict(response.headers)
        )
        
    except httpx.ConnectError:
        raise HTTPException(status_code=503, detail=f"Service '{service_name}' unavailable.")
    except HTTPException:
        raise 
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"An error occurred: {e}")