"""Loopback-only companion health endpoint. Put behind an operator-managed TLS reverse proxy."""
import hmac
import os
import re
from http.server import BaseHTTPRequestHandler, HTTPServer

def make_handler(token):
    class HealthHandler(BaseHTTPRequestHandler):
        def do_HEAD(self):
            supplied = self.headers.get("Authorization", "")
            valid = hmac.compare_digest(supplied.encode("utf-8"), ("Bearer " + token).encode("ascii"))
            code = 404 if self.path != "/health" else (204 if valid else 401)
            self.send_response(code)
            self.send_header("Cache-Control", "no-store")
            self.send_header("Content-Length", "0")
            self.send_header("Connection", "close")
            self.end_headers()
            self.close_connection = True

        def log_message(self, format, *args):
            # Do not log request paths or credentials.
            pass
    return HealthHandler

class HealthServer(HTTPServer):
    def get_request(self):
        conn, addr = super().get_request()
        conn.settimeout(4)
        return conn, addr

def main():
    token = os.environ.get("SCHIZO_COMPANION_TOKEN", "")
    if not re.fullmatch(r"[A-Za-z0-9_-]{32,256}", token):
        raise SystemExit("Set a valid SCHIZO_COMPANION_TOKEN")
    with HealthServer(("127.0.0.1", 8765), make_handler(token)) as server:
        server.serve_forever()

if __name__ == "__main__":
    main()
