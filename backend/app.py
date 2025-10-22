from flask import Flask, jsonify
from flask_cors import CORS
import pymysql

app = Flask(__name__)
CORS(app)  # 允许安卓访问

# 连接你的 RDS
conn = pymysql.connect(
    host="database-1.cjw0amswcib4.us-east-2.rds.amazonaws.com",
    user="admin",
    password="Test12345!", 
    database="dealtracker",
    cursorclass=pymysql.cursors.DictCursor
)

@app.route("/price/<int:pid>")
def get_price(pid):
    with conn.cursor() as cursor:
        sql = "SELECT * FROM price WHERE pid = %s"
        cursor.execute(sql, (pid,))
        rows = cursor.fetchall()
        return jsonify(rows)

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001)
