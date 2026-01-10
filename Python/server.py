import http.server
import socketserver
import urllib.request
import urllib.parse
import urllib.error
import ssl
import re

class SimpleProxy(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        proxy_host = self.headers.get('Host')
        raw_path = self.path[1:]
        
        # URLの正規化
        if raw_path.startswith("http"):
            target_url = raw_path
        else:
           
            target_url = "https://www.google.com/" + raw_path

        print(f"Requesting: {target_url}")

        try:
            ctx = ssl.create_default_context()
            ctx.check_hostname = False
            ctx.verify_mode = ssl.CERT_NONE

            headers = {
                'User-Agent': 'Mozilla/5.0 (Linux; U; Android 1.6; ja-jp; HT-03A Build/ERE27) AppleWebKit/530.17 (KHTML, like Gecko) Version/4.0 Mobile Safari/530.17',
                'Accept': '*/*'
            }

            req = urllib.request.Request(target_url, headers=headers)
            
            with urllib.request.urlopen(req, timeout=10, context=ctx) as response:
                content_type = response.getheader("Content-Type", "")
                body = response.read()
                
                self.send_response(200)
                self.send_header("Content-Type", content_type)
                self.end_headers()
                
                if "text/html" in content_type:
                    html = body.decode('utf-8', errors='ignore'
                    html = re.sub(r'src="//', 'src="https://', html)
                    
                    html = re.sub(r'(src|href)="https?://', rf'\1="http://{proxy_host}/https://', html)
                    self.wfile.write(html.encode('utf-8'))
                else:
                    self.wfile.write(body)

        except Exception as e:
            print(f"Error: {e}")
            self.send_error(500, str(e))

PORT = 8000
socketserver.TCPServer.allow_reuse_address = True
with socketserver.TCPServer(("", PORT), SimpleProxy) as httpd:
    print(f"Proxy started on port {PORT}")
    httpd.serve_forever()
