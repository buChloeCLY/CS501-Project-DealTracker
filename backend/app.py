from flask import Flask, jsonify, request
from flask_cors import CORS
import pymysql
from decimal import Decimal
import hashlib

app = Flask(__name__)
CORS(app)

# ---------------- DB CONFIG ----------------
DB_CONFIG = dict(
    host="database-1.cjw0amswcib4.us-east-2.rds.amazonaws.com",
    user="admin",
    password="Test12345!",
    database="dealtracker",
    cursorclass=pymysql.cursors.DictCursor,
    autocommit=True,
)

def get_conn():
    """ 为每一次请求单独返回一个连接，避免 read-of-closed-file """
    return pymysql.connect(**DB_CONFIG)


def _to_jsonable(rows):
    for r in rows:
        for k, v in r.items():
            if isinstance(v, Decimal):
                r[k] = float(v)
    return rows


def hash_password(password: str) -> str:
    """简单的密码哈希（生产环境建议使用 bcrypt）"""
    return hashlib.sha256(password.encode()).hexdigest()


# ---------------- API: 用户相关 ----------------

# 获取用户信息
@app.route("/user/<int:uid>", methods=["GET"])
def get_user(uid: int):
    try:
        conn = get_conn()
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT uid, name, email, gender, created_at, updated_at
                FROM user
                WHERE uid = %s
                """,
                (uid,)
            )
            user = cur.fetchone()
            if user:
                return jsonify(user), 200
            else:
                return jsonify({"error": "User not found"}), 404
    except Exception as e:
        print("SQL ERROR:", e)
        return jsonify({"error": str(e)}), 500
    finally:
        try:
            conn.close()
        except:
            pass


# 用户登录
@app.route("/user/login", methods=["POST"])
def login():
    try:
        data = request.get_json()
        email = data.get("email")
        password = data.get("password")
        
        if not email or not password:
            return jsonify({"error": "Email and password required"}), 400
        
        hashed_password = hash_password(password)
        
        conn = get_conn()
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT uid, name, email, gender
                FROM user
                WHERE email = %s AND password = %s
                """,
                (email, hashed_password)
            )
            user = cur.fetchone()
            if user:
                return jsonify({"success": True, "user": user}), 200
            else:
                return jsonify({"success": False, "error": "Invalid credentials"}), 401
    except Exception as e:
        print("SQL ERROR:", e)
        return jsonify({"error": str(e)}), 500
    finally:
        try:
            conn.close()
        except:
            pass


# 用户注册
@app.route("/user/register", methods=["POST"])
def register():
    try:
        data = request.get_json()
        name = data.get("name")
        email = data.get("email")
        password = data.get("password")
        gender = data.get("gender", "Prefer not to say")
        
        if not all([name, email, password]):
            return jsonify({"error": "Name, email and password required"}), 400
        
        hashed_password = hash_password(password)
        
        conn = get_conn()
        with conn.cursor() as cur:
            # 检查邮箱是否已存在
            cur.execute("SELECT uid FROM user WHERE email = %s", (email,))
            if cur.fetchone():
                return jsonify({"error": "Email already exists"}), 409
            
            # 插入新用户
            cur.execute(
                """
                INSERT INTO user (name, email, password, gender)
                VALUES (%s, %s, %s, %s)
                """,
                (name, email, hashed_password, gender)
            )
            uid = cur.lastrowid
            
            return jsonify({
                "success": True,
                "user": {
                    "uid": uid,
                    "name": name,
                    "email": email,
                    "gender": gender
                }
            }), 201
    except Exception as e:
        print("SQL ERROR:", e)
        return jsonify({"error": str(e)}), 500
    finally:
        try:
            conn.close()
        except:
            pass


# 更新用户信息
@app.route("/user/<int:uid>", methods=["PUT"])
def update_user(uid: int):
    try:
        data = request.get_json()
        name = data.get("name")
        email = data.get("email")
        gender = data.get("gender")
        password = data.get("password")
        
        conn = get_conn()
        with conn.cursor() as cur:
            # 检查用户是否存在
            cur.execute("SELECT uid FROM user WHERE uid = %s", (uid,))
            if not cur.fetchone():
                return jsonify({"error": "User not found"}), 404
            
            # 构建更新语句
            update_fields = []
            params = []
            
            if name:
                update_fields.append("name = %s")
                params.append(name)
            if email:
                update_fields.append("email = %s")
                params.append(email)
            if gender:
                update_fields.append("gender = %s")
                params.append(gender)
            if password:
                update_fields.append("password = %s")
                params.append(hash_password(password))
            
            if not update_fields:
                return jsonify({"error": "No fields to update"}), 400
            
            params.append(uid)
            query = f"UPDATE user SET {', '.join(update_fields)} WHERE uid = %s"
            cur.execute(query, tuple(params))
            
            # 返回更新后的用户信息
            cur.execute(
                """
                SELECT uid, name, email, gender, updated_at
                FROM user
                WHERE uid = %s
                """,
                (uid,)
            )
            user = cur.fetchone()
            
            return jsonify({"success": True, "user": user}), 200
    except pymysql.IntegrityError as e:
        return jsonify({"error": "Email already exists"}), 409
    except Exception as e:
        print("SQL ERROR:", e)
        return jsonify({"error": str(e)}), 500
    finally:
        try:
            conn.close()
        except:
            pass


# 删除用户
@app.route("/user/<int:uid>", methods=["DELETE"])
def delete_user(uid: int):
    try:
        conn = get_conn()
        with conn.cursor() as cur:
            cur.execute("DELETE FROM user WHERE uid = %s", (uid,))
            if cur.rowcount > 0:
                return jsonify({"success": True, "message": "User deleted"}), 200
            else:
                return jsonify({"error": "User not found"}), 404
    except Exception as e:
        print("SQL ERROR:", e)
        return jsonify({"error": str(e)}), 500
    finally:
        try:
            conn.close()
        except:
            pass


# ---------------- API: 价格相关 ----------------
@app.route("/price/<int:pid>")
def get_prices(pid: int):
    try:
        conn = get_conn()
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT p1.id, p1.pid, p1.price, p1.date, p1.platform, p1.idInPlatform, p1.link
                FROM price p1
                INNER JOIN (
                    SELECT platform, MAX(date) AS max_date
                    FROM price
                    WHERE pid = %s
                    GROUP BY platform
                ) p2 ON p1.platform = p2.platform AND p1.date = p2.max_date
                WHERE p1.pid = %s
                ORDER BY p1.price ASC
                """,
                (pid, pid),
            )
            rows = cur.fetchall()
            return jsonify(_to_jsonable(rows)), 200
    except Exception as e:
        print("SQL ERROR:", e)
        return jsonify([]), 200
    finally:
        try:
            conn.close()
        except:
            pass


@app.route("/history/<int:pid>")
def get_history(pid: int):
    days = request.args.get("days", default=7, type=int)

    try:
        conn = get_conn()
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT *
                FROM (
                    SELECT
                      DATE(`date`)                     AS d,
                      DATE_FORMAT(`date`, '%%m/%%d')  AS date,
                      MIN(price)                      AS price
                    FROM price
                    WHERE pid = %s
                    GROUP BY d
                    ORDER BY d DESC
                    LIMIT %s
                ) AS tmp
                ORDER BY d ASC
                """,
                (pid, days)
            )
            rows = cur.fetchall()
            out = [{"date": r["date"], "price": float(r["price"])} for r in rows]
            return jsonify(out), 200
    except Exception as e:
        print("SQL ERROR:", e)
        return jsonify([]), 200
    finally:
        try:
            conn.close()
        except:
            pass


# ---------------- FAVICON 避免 500 ----------------
@app.route("/favicon.ico")
def favicon():
    return "", 204


# ---------------- 错误处理 ----------------
@app.errorhandler(Exception)
def on_error(e):
    print("UNCAUGHT ERROR:", e)
    return jsonify({"error": "internal"}), 500


# ---------------- MAIN ----------------
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001)