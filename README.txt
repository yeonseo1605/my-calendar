My Calendar

실행 방법
1. 실행.vbs를 더블클릭합니다. 설치형 웹앱 기능을 사용할 수 있도록 로컬 서버로 실행됩니다.
2. 또는 index.html을 더블클릭합니다.

현재 동작하는 기능
- 월간/주간/일간/목록 보기
- 일정 등록, 수정, 삭제
- 달력에서 일정 끌어서 날짜 이동
- 반복 일정 범위 선택(이 일정만/이 일정 및 앞으로/전체 일정)
- 반복 요일, 종료일, 반복 횟수
- 이전 일정 제목·시간·주소 자동완성
- 35개 라벨의 개별 이름 변경
- 라이트/다크 테마 및 일정 색 표시 방식
- 다크 배경 계열 및 진하기 조절
- 위젯 크기/투명도/정보량 미리보기
- 네이버 지도 검색 링크
- 개인 할 일 등록/완료/삭제
- 주말 숨김 및 일정 간격 설정
- 브라우저 일정 알림
- 표준 ICS 캘린더 가져오기/내보내기
- JSON 전체 백업 및 복원
- 브라우저 로컬 저장

클라우드 동기화 준비
- Git/GitHub: 소스 코드와 배포만 담당합니다.
- Google Calendar: 일정 제목, 날짜, 시간, 주소, 참석자, 반복 일정의 기준입니다.
- Supabase: 라벨, 화면 설정, 할 일, Google 동기화 상태를 사용자별로 저장합니다.
- Supabase를 연결하기 전에는 기존처럼 브라우저 로컬 데이터만 사용합니다.

Supabase 연결 순서
1. Supabase에서 프로젝트를 만듭니다.
2. supabase/migrations/202610140001_initial_schema.sql을 SQL Editor에서 실행합니다.
3. config.example.js를 config.js로 복사합니다.
4. config.js에 Project URL과 publishable key를 입력합니다. 정적 사이트 배포에 이 파일이 포함되어야 합니다.
5. Supabase Authentication에서 Google 공급자와 배포 주소를 설정합니다.

보안 주의
- config.js에는 anon public key만 넣습니다.
- service_role 키, Google Client Secret, refresh token은 Git이나 브라우저 코드에 넣지 않습니다.
- publishable key는 RLS를 적용한 브라우저 앱에 포함할 수 있습니다. service_role/secret 키만 Git에서 제외합니다.
- .env 파일은 Git에서 제외되어 있습니다.

Google Calendar 무료 연동
- 외부 캘린더 설정에서 Google OAuth 웹 클라이언트 ID 입력
- Google 연결 후 지금 동기화 실행
- 로컬 일정 등록/수정/삭제 및 Google 일정 가져오기 지원
- 액세스 토큰은 저장하지 않으며 만료 시 다시 연결

외부 계정 또는 운영 서버 연결이 필요한 기능
- NAVER Maps API 지도/주소 자동검색
- 여러 기기 사이의 데이터 동기화
- Android/iPhone 실제 홈 화면 위젯

위 네 기능은 Google OAuth 정보, NAVER Cloud Maps 키, 배포 서버 및
앱 개발자 계정이 필요하므로 현재 로컬 파일만으로 자동 활성화할 수 없습니다.
