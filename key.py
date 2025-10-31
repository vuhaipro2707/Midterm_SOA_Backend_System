from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.backends import default_backend

# Kích thước khóa (2048-bit)
KEY_SIZE = 2048
# Lệnh 'exponent' OpenSSL tương đương với public_exponent
PUBLIC_EXPONENT = 65537

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
    # Tệp này là private_key.pem
    encryption_algorithm=serialization.NoEncryption()
)

# Lưu tệp private_key.pem
with open("private_key.pem", "wb") as f:
    f.write(private_key_pkcs8_pem)
    print("✅ Đã tạo tệp private_key.pem (PKCS#8).")

# --- 3. TẠO PUBLIC KEY TỪ PRIVATE KEY ---
# Tương đương: openssl rsa -pubout
public_key_pem = public_key.public_bytes(
    encoding=serialization.Encoding.PEM,
    # Standard format cho Public Key (SubjectPublicKeyInfo)
    format=serialization.PublicFormat.SubjectPublicKeyInfo
)

# Lưu tệp public_key.pem
with open("public_key.pem", "wb") as f:
    f.write(public_key_pem)
    print("✅ Đã tạo tệp public_key.pem.")

# --- 4. (Tùy chọn) Xóa khóa thô ---
# Trong code Python này, khóa thô chỉ tồn tại trong bộ nhớ (biến private_key)
# và không được ghi ra đĩa dưới dạng tệp private_rsa.pem, nên không cần xóa.
print("📌 Lưu ý: Khóa thô (private_rsa.pem) không được tạo ra tệp trên đĩa, nên không cần xóa.")