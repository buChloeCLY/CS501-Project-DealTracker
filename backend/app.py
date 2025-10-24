from flask import Flask, jsonify, request
from flask_cors import CORS
import pymysql
from decimal import Decimal

app = Flask(__name__)
CORS(app)

# ---------------- DB CONNECTION ----------------
# 放到 try 中，避免启动时数据库无响应导致服务挂掉
try:
    conn = pymysql.connect(
        host="database-1.cjw0amswcib4.us-east-2.rds.amazonaws.com",
        user="admin",
        password="Test12345!",
        database="dealtracker",
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=True,
    )
except Exception as e:
    # 数据库连不上，后续查询接口仍返回空数据，不影响服务启动
    conn = None


# ---------------- UTIL ----------------
def _to_jsonable(rows):
    """把 Decimal 转成 float"""
    for r in rows:
        for k, v in r.items():
            if isinstance(v, Decimal):
                r[k] = float(v)
    return rows


# ---------------- API: 平台价格原始数据 ----------------
@app.route("/price/<int:pid>")
def get_prices(pid: int):
    # 如果 conn = None（数据库未连接） → 直接返回空数组
    if conn is None:
        return jsonify([]), 200

    try:
        conn.ping(reconnect=True)
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
            return jsonify(_to_jsonable(rows))
    except Exception as e:
        # 线上容错：不把错误传到前端
        return jsonify([]), 200


# ---------------- API: 历史最低价（折线图） ----------------
@app.route("/history/<int:pid>")
def get_history(pid: int):
    days = request.args.get("days", default=7, type=int)

    if conn is None:
        return jsonify([]), 200

    try:
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
        print("SQL ERROR:", e)  # 临时调试用，你之后也可以删掉
        return jsonify([]), 200

# ---------------- ERROR HANDLER ----------------
@app.errorhandler(Exception)
def on_error(e):
    # 不泄露细节给客户端
    return jsonify({"error": "internal"}), 500




# ---------------- MAIN ----------------
if __name__ == "__main__":
    # 安卓可以访问必须是 host=0.0.0.0
    app.run(host="0.0.0.0", port=5001)
