# Test API cho User Profile & Preferences

BASE_URL="http://localhost:8082"

## 1. Đăng ký user mới (nếu chưa có)
echo "=== REGISTER ==="
curl -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "testuser@example.com",
    "password": "password123"
  }'

echo -e "\n\n=== LOGIN ==="
# 2. Login để lấy JWT token
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }')

echo $LOGIN_RESPONSE

# Lấy access token từ response
TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
echo "Token: $TOKEN"

echo -e "\n\n=== GET PROFILE ==="
# 3. Lấy thông tin profile hiện tại
curl -X GET "$BASE_URL/api/user/profile" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"

echo -e "\n\n=== UPDATE PROFILE ==="
# 4. Cập nhật profile
curl -X PUT "$BASE_URL/api/user/profile" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+84123456789",
    "address": "123 Nguyen Hue, Ho Chi Minh City",
    "gender": "male",
    "travelStyle": "Adventure",
    "interests": ["ADVENTURE", "NATURE", "FOOD"],
    "bio": "Love traveling and exploring new places!"
  }'

echo -e "\n\n=== GET UPDATED PROFILE ==="
# 5. Lấy lại profile sau khi update
curl -X GET "$BASE_URL/api/user/profile" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"

echo -e "\n\n=== GET PREFERENCES ==="
# 6. Lấy preferences hiện tại
curl -X GET "$BASE_URL/api/user/preferences" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"

echo -e "\n\n=== UPDATE PREFERENCES ==="
# 7. Cập nhật preferences
curl -X PUT "$BASE_URL/api/user/preferences" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "emailNotifications": true,
    "pushNotifications": true,
    "smsNotifications": false,
    "profileVisibility": true,
    "language": "vi",
    "timezone": "Asia/Ho_Chi_Minh",
    "darkMode": true
  }'

echo -e "\n\n=== GET UPDATED PREFERENCES ==="
# 8. Lấy lại preferences sau khi update
curl -X GET "$BASE_URL/api/user/preferences" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"

echo -e "\n\n=== PARTIAL UPDATE PROFILE ==="
# 9. Test partial update (chỉ update bio)
curl -X PUT "$BASE_URL/api/user/profile" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "bio": "Updated bio - I love Vietnam!"
  }'

echo -e "\n\n=== PARTIAL UPDATE PREFERENCES ==="
# 10. Test partial update preferences (chỉ update darkMode)
curl -X PUT "$BASE_URL/api/user/preferences" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "darkMode": false
  }'

echo -e "\n\nDone!"
