# My Calendar Android 위젯

Google·네이버 캘린더처럼 홈 화면에서 일정을 빠르게 확인하는 Android 위젯입니다.

## 표시 방식

- 작은 크기: 오늘 날짜와 가장 가까운 오늘 일정
- 가로형 크기: 이번 주 7일과 날짜별 첫 일정 제목
- 중간 크기: 월간 달력에 날짜별 첫 일정 제목과 추가 일정 개수
- 큰 크기: 일정 제목이 표시되는 월간 달력과 향후 일정 목록
- 홈 화면에서 가로·세로 크기를 조절하면 자동으로 형태가 바뀝니다.
- 주간 위젯은 이전 주·다음 주·오늘, 월간 위젯은 이전 달·다음 달·오늘 버튼을 지원합니다.
- 한국 휴일 캘린더는 날짜를 빨간색으로 표시하되 일정 제목에는 중복 표시하지 않습니다.

## 데이터

Android의 캘린더 저장소를 읽습니다. 휴대폰 설정에서 Google Calendar 동기화를 켜면 같은 Google 일정을 위젯에서 볼 수 있습니다. 임의의 예시 일정은 표시하지 않습니다.

## 설치

1. 프로젝트 최상단의 `MyCalendar-Android-Widget-debug.apk`를 휴대폰으로 옮겨 설치합니다.
2. My Calendar 앱을 열고 `캘린더 권한 허용`을 누릅니다.
3. 홈 화면을 길게 누르고 `위젯` → `My Calendar`를 선택합니다.
4. 위젯 테두리를 끌어 크기를 조절합니다.

## 빌드

JDK 17과 Android SDK 35를 사용합니다.

```powershell
cd android
.\gradlew.bat assembleDebug
```

생성 파일: `app/build/outputs/apk/debug/app-debug.apk`
