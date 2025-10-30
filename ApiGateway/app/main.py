from fastapi import FastAPI, Request, HTTPException
from fastapi.responses import JSONResponse
import httpx
from jose import jwt, JWTError
import os
from json.decoder import JSONDecodeError
from urllib.parse import quote

app = FastAPI(title="API Gateway")

PUBLIC_KEY_PATH = os.path.join(os.path.dirname(__file__), "public_key.pem")
ALGORITHM = "RS256"

SYSTEM_SERVICES = {
    "auth": "http://auth-service:8081", 
    "customer": "http://customer-management-service:8082",
    "payment": "http://payment-processor-service:8083",
    "tuition": "http://tuition-service:8084",
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