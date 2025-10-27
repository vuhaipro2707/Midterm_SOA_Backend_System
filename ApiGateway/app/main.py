from fastapi import FastAPI, Request, HTTPException
from fastapi.responses import JSONResponse
import httpx

app = FastAPI(title="API Gateway")


SYSTEM_SERVICES = {
    "tuition": "http://tuition-service:8081",
    "payment": "http://payment-processor-service:8082",
    "account": "http://customer-account-service:8083", 
}

# Khởi tạo httpx client cho các request bất đồng bộ
# timeout=None là để chờ phản hồi của system, có thể đặt giá trị cụ thể
client = httpx.AsyncClient(timeout=None)

# ----------------------------------------------------
# Endpoint cho Health Check của Gateway
# ----------------------------------------------------
@app.get("/health")
async def health_check():
    return {"status": "Gateway is running."}

# ----------------------------------------------------
# Logic Định Tuyến (Catch-all Proxy)
# ----------------------------------------------------
# Sử dụng @app.api_route("/{path:path}"). Nó sẽ bắt tất cả các request đến.
@app.api_route("/{path:path}", methods=["GET", "POST", "PUT", "DELETE", "PATCH"])
async def proxy_requests(path: str, request: Request):
    
    # 1. Xác định dịch vụ mục tiêu dựa trên đường dẫn
    # Ví dụ: /system-a/users -> service_name = system-a
    #         /system-b/products -> service_name = system-b
    try:
        service_name, sub_path = path.split("/", 1)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid path format. Expecting /<service_name>/<path>")
    
    if service_name not in SYSTEM_SERVICES:
        raise HTTPException(status_code=404, detail=f"Service '{service_name}' not found.")
        
    base_url = SYSTEM_SERVICES[service_name]
    
    # 2. Xây dựng URL đích
    # URL đích: http://system-a:8081/users?param=value
    target_url = f"{base_url}/{sub_path}"
    
    # 3. Sao chép header và body
    headers = dict(request.headers)
    
    # Lấy body (dùng .json() cho POST/PUT/PATCH)
    try:
        json_data = await request.json()
    except:
        json_data = None
        
    # 4. Gửi request đến System mục tiêu
    try:
        # Gửi request bằng phương thức gốc (GET, POST, PUT, v.v.)
        response = await client.request(
            method=request.method,
            url=target_url,
            headers=headers,
            json=json_data,
            params=request.query_params
        )
        
        # 5. Trả về response từ System mục tiêu cho Client
        return JSONResponse(
            content=response.json(),
            status_code=response.status_code,
            headers=response.headers
        )
        
    except httpx.ConnectError:
        # Xử lý khi không thể kết nối đến System
        raise HTTPException(status_code=503, detail=f"Service '{service_name}' unavailable.")
    except Exception as e:
        # Xử lý lỗi khác
        raise HTTPException(status_code=500, detail=f"An error occurred: {e}")