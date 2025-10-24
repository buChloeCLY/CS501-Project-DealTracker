from flask import Flask, jsonify, request
from flask_cors import CORS
import pymysql
from decimal import Decimal

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


# ---------------- API: 价格原始数据 ----------------
@app.route("/price/<int:pid>")
def get_prices(pid: int):
    try:
        conn = get_conn()
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT id, pid, price, date, platform, idInPlatform, link
                FROM price
                WHERE pid = %s
                ORDER BY date ASC
                """,
                (pid,),
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


# ---------------- API: 历史价格（折线图） ----------------
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
