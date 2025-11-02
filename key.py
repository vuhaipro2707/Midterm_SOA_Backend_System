import os
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.backends import default_backend

# Định nghĩa đường dẫn tương đối để lưu khóa
# Private Key được lưu trong auth-service/src/main/resources/keys/
PRIVATE_KEY_PATH = "auth-service/src/main/resources/keys/private_key.pem"
# Public Key được lưu trong ApiGateway/app/
PUBLIC_KEY_PATH = "ApiGateway/app/public_key.pem"

# Kích thước khóa (2048-bit)
KEY_SIZE = 2048
# Lệnh 'exponent' OpenSSL tương đương với public_exponent
PUBLIC_EXPONENT = 65537

def create_directory(path):
    """Tạo thư mục của đường dẫn nếu nó chưa tồn tại."""
    dir_name = os.path.dirname(path)
    if dir_name and not os.path.exists(dir_name):
        os.makedirs(dir_name)
        print(f"📁 Đã tạo thư mục: {dir_name}")

# --- 1. TẠO PRIVATE KEY THÔ (Dạng RSA) và PUBLIC KEY ---
# cryptography tạo cả hai cùng lúc
private_key = rsa.generate_private_key(
    public_exponent=PUBLIC_EXPONENT,
    key_size=KEY_SIZE,
    backend=default_backend()
)

public_key = private_key.public_key()

# --- 2. CHUYỂN ĐỔI PRIVATE KEY SANG ĐỊNH DẠNG PKCS#8 (Không mã hóa) ---
# Tương đương: openssl pkcs8 -topk8 -nocrypt
private_key_pkcs8_pem = private_key.private_bytes(
    encoding=serialization.Encoding.PEM,
    format=serialization.PrivateFormat.PKCS8,
    # Mã hóa (encryption_algorithm) đặt là NoEncryption() tương đương với -nocrypt
    encryption_algorithm=serialization.NoEncryption()
)

# Lưu tệp private_key.pem
create_directory(PRIVATE_KEY_PATH)
with open(PRIVATE_KEY_PATH, "wb") as f:
    f.write(private_key_pkcs8_pem)
    print(f"✅ Đã tạo tệp Private Key (PKCS#8) tại: {PRIVATE_KEY_PATH}")

# --- 3. TẠO PUBLIC KEY TỪ PRIVATE KEY ---
# Tương đương: openssl rsa -pubout
public_key_pem = public_key.public_bytes(
    encoding=serialization.Encoding.PEM,
    # Standard format cho Public Key (SubjectPublicKeyInfo)
    format=serialization.PublicFormat.SubjectPublicKeyInfo
)

# Lưu tệp public_key.pem
create_directory(PUBLIC_KEY_PATH)
with open(PUBLIC_KEY_PATH, "wb") as f:
    f.write(public_key_pem)
    print(f"✅ Đã tạo tệp Public Key tại: {PUBLIC_KEY_PATH}")

print("📌 Lưu ý: Việc tạo khóa hoàn tất.")