package webserver;

import db.MemoryUserRepository;
import model.User;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestHandler implements Runnable {
    private final Socket connection; // 클라이언트 연결 소켓
    private static final Logger log = Logger.getLogger(RequestHandler.class.getName());
    private static MemoryUserRepository memoryUserRepository = MemoryUserRepository.getInstance();

    public RequestHandler(Socket connection) {
        this.connection = connection; // 소켓 초기화
    }

    @Override
    public void run() {
        log.log(Level.INFO, "New Client Connect! Connected IP : " + connection.getInetAddress() + ", Port : " + connection.getPort());
        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(in)); // 입력 스트림
            DataOutputStream dos = new DataOutputStream(out); // 출력 스트림

            // 요청 라인 읽기
            String request = br.readLine();
            log.info("Request Line: " + request); // 요청 라인 로그 출력

            if (request != null) {
                String[] requestParts = request.split(" "); // 요청 라인 파싱
                if (requestParts.length == 3) {
                    String method = requestParts[0]; // HTTP 메서드
                    String path = requestParts[1]; // 요청 경로

                    // HTTP 메서드에 따른 요청 처리
                    if ("GET".equalsIgnoreCase(method)) {
                        handleGetRequest(dos, path);
                    } else if ("POST".equalsIgnoreCase(method)) {
                        handlePostRequest(br, dos, path);
                    }
                }
            }

        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage()); // 예외 발생 시 로그 기록
        }
    }

    private void handlePostRequest(BufferedReader br, DataOutputStream dos, String path) throws IOException {

        String line;
        int contentLength = 0;
        // 헤더에서 Content-Length 읽기
        while (!(line = br.readLine()).isEmpty()) {
            if (line.startsWith("Content-Length:")) { // Content-Length 헤더 확인
                contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim()); // 길이 추출
            }
        }

        // 본문 읽기
        char[] body = new char[contentLength];
        br.read(body, 0, contentLength);
        String postData = new String(body); // 본문을 문자열로 변환

        // POST 데이터 처리 (여기서 로직 구현 가능)
        log.info("Received POST data: " + postData); // 받은 POST 데이터 로그 출력

        // 쿼리 스트링 파라미터 파싱
        String[] paramValues = new String[4]; // 저장할 배열 크기
        int index = 0;

        // 쿼리 스트링이 비어있지 않은 경우 파라미터 분리
        if (!postData.isEmpty()) {
            String[] params = postData.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=", 2);
                String key = keyValue[0]; // 키 추출
                String value = keyValue.length > 1 ? keyValue[1] : ""; // 값 추출

                // value 값을 String 배열에 저장
                if (index < paramValues.length) {
                    paramValues[index] = value; // 배열에 값 저장
                    index++;
                }
            }
            if(path.equals("/user/signup")){
                // User 객체 생성 후 저장
                User user = new User(paramValues[0], paramValues[1], paramValues[2], paramValues[3]);
                memoryUserRepository.addUser(user); // 사용자 저장소에 사용자 추가
            }
            if(path.equals("/user/login")){

            }
        }
        sendRedirect(dos, "/index.html"); // 리다이렉트
    }

    private void handleGetRequest(DataOutputStream dos, String path) throws IOException {
        // 요청 경로에 따른 HTML 파일 전송
        if ("/".equals(path) || "/index.html".equals(path)) {
            SendHTML(dos, "index.html");
        }
        if ("/user/form.html".equals(path)) {
            SendHTML(dos, "/user/form.html");
        }
        if ("/user/login.html".equals(path)) {
            SendHTML(dos, "/user/login.html");
        }
        // 회원가입 GET 버전
        if (path.startsWith("/user/signup")) {
            // 경로에서 쿼리 스트링 분리
            String[] parts = path.split("\\?", 2);
            String queryString = (parts.length > 1) ? parts[1] : ""; // 쿼리 스트링 추출

            // 쿼리 스트링 파라미터 파싱
            String[] paramValues = new String[4]; // 저장할 배열 크기
            int index = 0;

            // 쿼리 스트링이 비어있지 않은 경우 파라미터 분리
            if (!queryString.isEmpty()) {
                String[] params = queryString.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=", 2);
                    String key = keyValue[0]; // 키 추출
                    String value = keyValue.length > 1 ? keyValue[1] : ""; // 값 추출

                    // value 값을 String 배열에 저장
                    if (index < paramValues.length) {
                        paramValues[index] = value; // 배열에 값 저장
                        index++;
                    }
                }
                // User 객체 생성 후 저장
                User user = new User(paramValues[0], paramValues[1], paramValues[2], paramValues[3]);
                memoryUserRepository.addUser(user); // 사용자 저장소에 사용자 추가
            }
            sendRedirect(dos, "/index.html"); // 리다이렉트
        }
    }

    private void SendHTML(DataOutputStream dos, String fileName) throws IOException {
        Path filePath = Paths.get("webapp", fileName); // 파일 경로 설정
        // 파일이 존재하는 경우 HTML 파일 전송
        if (Files.exists(filePath)) {
            byte[] fileContent = Files.readAllBytes(filePath); // 파일 내용 읽기
            response200Header(dos, fileContent.length); // 200 OK 헤더 응답
            responseBody(dos, fileContent); // 응답 본문 전송
        }
    }

    //302 status code를 적용하기 위한 메소드
    private void sendRedirect(DataOutputStream dos, String redirectUrl) {
        try {
            // 리다이렉트 응답 작성
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: " + redirectUrl + "\r\n"); // 리다이렉션 URL
            dos.writeBytes("Content-Length: 0 \r\n"); // 본문 길이
            dos.writeBytes("\r\n");
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace(); // 예외 발생 시 스택 트레이스 출력
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            // 200 OK 헤더 작성
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n"); // 콘텐츠 타입 설정
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n"); // 본문 길이
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage()); // 예외 발생 시 로그 기록
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            // 본문 전송
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage()); // 예외 발생 시 로그 기록
        }
    }
}
