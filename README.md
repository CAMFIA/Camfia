# <img src="./img/logo.png" width="50" height="50"> CAMFIA
화상으로 즐기는 마피아 게임

<br>

## 👩‍👩‍👧‍👦 팀원 소개
#### 🧑‍💻 <strong>유태규</strong> - 팀장, FE
#### 🧑‍💻 <strong>김지훈</strong> - BE, 배포
#### 🧑‍💻 <strong>김용훈</strong> - BE
#### 🧑‍💻 <strong>이현정</strong> - BE
#### 🧑‍💻 <strong>최은송</strong> - FE

<br>

## 📝 프로젝트 개요 
* <strong>진행 기간</strong>: 2021.07.12 ~ 2021.08.20
* <strong>목표</strong>
  * 플레이어는 음성과 영상을 통해 서로 소통하며, 오프라인으로만 즐기던 마피아 게임을 온라인으로 즐길 수 있습니다.
  * 어플리케이션을 설치하지 않고도 웹 브라우저로 간편하게 접속해 어디서든 CAMFIA를 이용할 수 있습니다.
  * 회원가입을 하지 않아도 구글 로그인만 하면 쉽게 방을 생성할 수 있습니다.
  * URL로 친구들을 초대하면 로그인의 번거로움 없이 게임을 즐길 수 있습니다. 
  * 재미있는 애니메이션과 사용자 친화적인 UI/UX를 설계해 게임의 몰입감을 높입니다.
  <br>


## ✍️ 프로젝트 소개 
<strong>CAMFIA</strong>는 온라인으로 마피아 게임을 즐길 수 있는 화상 기반 마피아 웹 게임입니다.
코로나 19 바이러스로 인해 언택트 시대에 들어서며 랜선으로 할 수 있는 게임, 회식, 놀이, 수다 문화 등의 인기가 높아졌습니다. 현재까지는 ZOOM이나 웹엑스 등의 기존 화상채팅 도구를 이용하여 게임을 했습니다. 그러나 미팅 기능에 초점이 맞춰진 어플리케이션에서는 게임 진행에 한계가 있었습니다. 그래서 <strong>"CAMFIA"</strong>는 마피아 게임 기능에 초점을 맞춘 화상 채팅 서비스를 제공하고자 합니다. 

### 게임 규칙

- 인원 수에 따른 직업 분배
  - <strong>최소 4인</strong>이 갖춰져야 게임 시작 가능
  - 4인의 경우 특수직업 없이 마피아 1, 시민 3으로 시작
  - 5인의 경우 특수직업 의사 추가, 의사1, 마피아1, 시민3으로 시작
  - 6인부터는 특수직업 의사와 경찰 모두 등장, 의사1, 경찰1, 마피아 (인원수//3), 시민 (나머지)로 시작

- 승리조건 
  - 마피아의 경우: 시민의 수가 마피아와 같아지면 게임 승리
  - 시민, 의사, 경찰의 경우: 모든 마피아를 제거하면 게임 승리, 혹은 제한된 턴이 15턴 안에 게임이 끝나지 않을 경우 승리

- 투표 규칙
  - 투표의 종류는 크게 3가지
    - `낮 투표`: 살아있는 모든 사람이 참가할 수 있는 투표로 여기서 최다득표자는 최종투표 후보가 됨
    - `최종 투표`: 최종투표 후보자를 제거할지 아닐지를 정하는 투표
    - `밤 투표`: 시민의 제외한 마피아, 경찰, 의사가 하는 투표로 각각의 직업마다 다른 효과를 지님
      - 마피아: 동료끼리 소통해 시민을 제거할 수 있음
      - 경찰: 투표한 사람의 직업을 알 수 있음
      - 의사: 투표한 사람은 이번 밤 투표에서 살릴 수 있음
  - 모든 투표는 아래와 같은 규칙을 따름
    - 각 페이즈별 남아있는 시간동안 마음껏 투표대상을 바꿀 수 있으며, 내가 한 투표를 취소할 수도 있음
    - 기본적으로 투표하지 않는다면 이는 무효표를 던진 것으로 간주
    - 낮 투표는 유기명 투표로 자신이 누구에게 투표했는지를 실시간으로 반영
    - 밤 투표의 경우 마피아끼리만 자신이 누구에게 투표했는지를 실시간으로 반영
    - 무효표와 동률에 관한 조항
      - 기본적으로 무효표가 과반이 넘어가면 그 투표는 무효로 처리하고 다음 페이즈로 넘어간다.
      - 낮 투표에서 생존자가 5인 이하일경우 2명, 생존자가 6인 이상일경우 3명까지 최종투표 후보에 올라갈 수 있다.
      - 밤투표와 최종투표의 경우 동률이 나오면 투표결과를 무효로 처리하고 진행한다.

### 게임 FLOW
1. `시작`: 각 플레이어는 자신의 직업이 무엇인지 받아보게 됩니다.
2. `낮 투표`: 각 플레이어는 의심스러운 사람을 투표 할 수 있습니다.
3. `최종 투표`: 최종 변론자는 주어진 시간 동안 변론하게 됩니다. 나머지 플레이어는 최종 변론자에게 <strong>KILL/SAVE</strong> 투표합니다. 낮 투표가 무효처리될 경우 스킵됩니다.
4. `최종 투표 결과 발표`: 최종 투표 결과를 확인합니다.
5. `밤 투표`: 의사, 마피아, 경찰은 각각 투표를 진행하게 됩니다.
6. `밤 투표 결과 발표`: 의사, 마피아, 경찰의 투표 결과를 확인합니다.
7. `사망 시`: 사망시 <strong>관전자 모드</strong>가 되어 오디오와 비디오가 강제로 꺼집니다. 관전자는 모든 플레이어의 오디오와 비디오를 볼 수 있으며 직업 또한 알 수 있습니다.
8. `종료`: 어느측이 승리했는지를 발표하게 되며, 대기 상태로 다시 돌아가게 됩니다.

<br>

## 💡 주요 기능 
### 구글 로그인 후 방 생성
> CAMFIA는 회원가입을 따로 하지 않아도 기존 구글 아이디로 로그인하면 바로 방을 만들 수 있습니다.

![google-login](https://user-images.githubusercontent.com/24693833/130022798-22acb98e-9ebe-4562-b822-4195be7aadee.gif)

<br>

![create-and-enter-room](https://user-images.githubusercontent.com/24693833/130015175-ff85bc5c-0f4e-42f6-817b-829de97fe6d4.gif)
- 게임 모드를 선택하면 방을 생성할 수 있습니다.
- 닉네임을 입력하고 방에 입장할 수 있습니다.
- 방장은 url을 통해 친구를 초대할 수 있습니다.

<br>

### 화상 미팅 기능
> openvidu를 활용해 화상 미팅을 구현했습니다. 플레이어들은 대화를 통해 싸피아 게임을 플레이할 수 있습니다.

![group-video-call](./img/group-video-call.gif)


- 동시에 말할 때 헷갈리지 않도록 현재 말하는 사람이 누구인지 표시됩니다.


### 마피아 게임  기능

> 실제 친구들과 모여서 했던 것처럼 말하고 투표하고 심리전을 펼칠 수 있습니다.
- WebSocket을 이용해 마피아 게임 기능을 구현했습니다.

- 낮 투표기능
  - 투표하고 싶은 사람을 클릭하면 투표하실 수 있습니다.
  - 제한시간 안에는 투표를 바꿀수도 있고 취소할 수 있습니다.
  - 더 이상 바꿀 마음이 없다면 투표확정 버튼을 클릭해 확정할 수 있습니다.

<br>

![mafia-at-night](./img/mafia-at-night.gif)

- 밤이 되면 플레이어는 자신의 직업에 맞는 역할을 수행할 수 있습니다(낮 투표와 동일한 방식으로 진행됩니다).
  - 관전자는 모든 플레이어의 화상과 음성을 확인할 수 있습니다.
  - 마피아는 화상과 음성을 통해 누굴 제거할 지 서로 이야기를 나누며 제거 대상을 선택할 수 있습니다(관전자를 제외한 마피아가 아닌 플레이어에게는 화상과 음성이 노출되지 않습니다).
  - 경찰은 마피아인지 확인할 대상을 선택할 수 있습니다(관전자를 제외한 다른 플레이어에게는 화상과 음성이 노출되지 않습니다).
  - 의사는 마피아에게 제거될 대상을 예상 및 선택하여 살릴 수 있습니다(관전자를 제외한 다른 플레이어에게는 화상과 음성이 노출되지 않습니다).
  

<br>

![observer-at-night](./img/observer-at-night.gif)

- 관전기능
  - 사망 시, 아무 역할 없이 게임이 진행되는 것을 보기만 하는 것이 아니라 다른 사람들의 직업이 무엇인지 확인할 수 있고, 무슨 얘기를 하고 있는지 들을 수 있습니다.

<br>

- 비디오 온오프 기능
  - 진짜 모여서 마피아를 하는 것처럼 밤이 되면 다른 사람의 영상이나 오디오를 듣지 못하게 되고, 특정 사람들끼리만 확인할 수 있도록 됩니다.

<br>


## ⚙️ 기술 스택 

<img src="./img/tech-stack-and-service-architecture.png" width="640">


- frontend
  - vue 3.1.5
  - vuex 4.0.2
  - vue-router 4.0.10
  - sockjs-client 1.5.1, webstomp-client 1.2.6
- backend
  - MariaDB 10.4.20
  - redis 6.2.4
  - spring Boot 2.5.2
- 화상미팅 관련
  - openvidu-browser 2.19.0
  - openvidu 2.19.0
- 배포
  - aws
  - nginx
  - jenkins

<br>

## 🏑 conventions
### commit message convention
- angular commit message convention 
  - type(scope): short summary
  - type 종류
    - feat : 새로운 기능 추가
    - fix : 버그 수정
    - docs : 문서 관련
    - style : 스타일 변경 (포매팅 수정, 들여쓰기 추가, …)
    - refactor : 코드 리팩토링
    - test : 테스트 관련 코드
    - build : 빌드 관련 파일 수정
    - ci : CI 설정 파일 수정
    - perf : 성능 개선
  - short summary
    - 마침표 쓰지 않기
    - 한글 영문, 모두 가능

### git branch convention

- master - develop - feature/bug
- 개인 개발한 것들은 모두 feature 또는 bug 브랜치에서 수행
- 프론트와 백에서 asignee로 지명된 이들이 심사후 develop으로 merge
- master에 merge할 경우 팀원 모두 모여서 회의 후 진행


<br>

## 이미지 빌드 방법
production 배포 목적의 nginx 이미지 빌드를 하기 위해서는 `frontend/.env.prod` 파일이 필요합니다. `frontend/.env.prod`의 내용은 다음과 같습니다.
```env
NODE_ENV = "production"
VUE_APP_BASE_URL = "https://my-app.com"
```

<br>

docker buildkit을 위해 `docker/dockerfile:1` 이미지를 미리 pull 합니다.
```sh
docker pull docker/dockerfile:1
```

<br>

다음 명령어를 통해 이미지를 빌드합니다.
```sh
DOCKER_BUILDKIT=1 docker build -t camfia/backend ./backend
DOCKER_BUILDKIT=1 docker build -t camfia/nginx ./frontend
```

## 배포 이미지 관리
다음 2개의 배포 이미지가 관리됩니다.
- ${REGISTRY}/camfia/backend
- ${REGISTRY}/camfia/nginx

이미지를 push 또는 pull하기 위해서는 docker login이 필요합니다.
```sh
docker login ${REGISTRY}
Username:
Password:
```

<br>

## 🌐 배포 방법
먼저, 서버가 2대 준비되어 있어야 합니다.
- openvidu 서버
- application 서버

<br>

### openvidu 서버 구축
[openvidu에서 제공하는 on-premises 방식](https://docs.openvidu.io/en/2.19.0/deployment/ce/on-premises/)을 참고하여 설치합니다.

<br>

#### **[전제 조건](https://docs.openvidu.io/en/2.19.0/deployment/ce/on-premises/#1-prerequisites)**
- Docker가 설치되어 있어야 합니다([설치 방법](https://docs.docker.com/engine/install/debian/#install-using-the-repository))
- `1.24` 이상 버전의 Docker Compose가 설치되어 있어야 합니다([설치 방법](https://docs.docker.com/compose/install/))
- domain 이름 : https를 사용하기 때문에 domain 이름이 필요합니다
- port 구성
  - 다음 port는 열려 있어야 합니다.
    - 22 TCP: to connect using SSH to admin OpenVidu.
    - 80 TCP: if you select Let's Encrypt to generate an SSL certificate this port is used by the generation process.
    - 443 TCP: OpenVidu server and application are published by default in standard https port.
    - 3478 TCP+UDP: used by TURN server to resolve clients IPs.
    - 40000 - 57000 TCP+UDP: used by Kurento Media Server to establish media connections.
    - 57001 - 65535 TCP+UDP: used by TURN server to establish relayed media connections.
  - 그 외의 모든 port는 닫혀 있어야 합니다.
  - 다음 port에 대해서는 서버 안에서 다른 프로세스가 사용하지 않도록 한다. 다른 프로세스가 해당 port를 사용하게 되면 openvidu가 정상 동작하지 않을 수 있습니다.
    - 80 : nginx(HTTP)
    - 443 : nginx(HTTPS)
    - 3478 : TRUN server
    - 5442 : openvidu based applicaiton
    - 5443 : openvidu server
    - 6379 : redis for openvidu
    - 8888 : KMS

<br>

#### [openvidu 설치](https://docs.openvidu.io/en/2.19.0/deployment/ce/on-premises/#2-deployment)
먼저 root 권한이 필요합니다.
```sh
sudo su
```

<br>

openvidu 설치 위치인 `/opt`로 이동합니다.
```sh
cd /opt
```

<br>

다음 명령어를 통해 openvidu를 설치합니다.
```sh
curl https://s3-eu-west-1.amazonaws.com/aws.openvidu.io/install_openvidu_latest.sh | bash
```

<br>

`/opt` 하위에 openvidu 디렉터리가 생성됩니다. 해당 디렉터리로 이동합니다.
```sh
cd /opt/openvidu
```

<br>

`.env` 파일에서 다음 항목들을 설정합니다.
```sh
DOMAIN_OR_PUBLIC_IP=my-openvidu-server.com
OPENVIDU_SECRET=MY_OPNEVIDU_SECRET

# Certificate type:
# - selfsigned:  Self signed certificate. Not recommended for production use.
#                Users will see an ERROR when connected to web page.
# - owncert:     Valid certificate purchased in a Internet services company.
#                Please put the certificates files inside folder ./owncert
#                with names certificate.key and certificate.cert
# - letsencrypt: Generate a new certificate using letsencrypt. Please set the
#                required contact email for Let's Encrypt in LETSENCRYPT_EMAIL
#                variable.
CERTIFICATE_TYPE=letsencrypt

LETSENCRYPT_EMAIL=example@email.com
```

`CERTIFICATE_TYPE`을 `letsencrypt`로 설정을 하기 위해서 `LETSENCRYPT_EMAIL`을 유효한 email로 설정해야 합니다.
그리고 80 포트를 꼭 열어주어야 인증서를 받을 수 있습니다.

<br>

`/opt/openvidu` 위치에서 다음 명령어로 실행할 수 있습니다. 그 외의 명렁어는 [공식 reference 페이지](https://docs.openvidu.io/en/2.19.0/deployment/ce/on-premises/#5-administration)에서 확인할 수 있습니다.
```sh
./openvidu start
```

<br>

참고로, `CERTIFICATE_TYPE`을 `letsencrypt`로 사용한다면 .env 파일에서 HTTPS_PORT를 변경하더라도 적용되지 않으니 default값인 443을 사용해야 합니다. 변경을 원한다면 [해당 reference 페이지](https://docs.openvidu.io/en/2.19.0/deployment/deploying-openvidu-apps/#in-a-different-port-as-openvidu ) 참고하면 됩니다.

<br>

#### openvidu call application 제거 방법
<https://docs.openvidu.io/en/2.19.0/deployment/deploying-openvidu-apps/#remove-openvidu-call-application>을 참고하였습니다.

<br>

openvidu call application이 설치 시에 같이 설치되어 openvidu 실행시에도 같이 실행됩니다.

<br>

해당 app을 삭제하기 위해서 우선 openvidu를 종료합니다.

```sh
cd /opt/openvidu
./openvidu stop
```

<br>

`docker-compose.override.yml`을 삭제합니다.

```sh
rm docker-compose.override.yml
```

<br>

openvidu를 다시 실행합니다.

```sh
 ./openvidu start
```

<br>

#### openvidu based application 배포 방법
<https://docs.openvidu.io/en/2.19.0/deployment/deploying-openvidu-apps/#deploy-other-openvidu-based-application>을 참고하였습니다.

<br>

다음 경로에 대해서는 사용하면 안 됩니다.
- /openvidu/
- /dashboard/ (only in OpenVidu CE)
- /inspector/ (only in OpenVidu Pro)

<br>

dockerized가 되어 있다면 `/opt/openvidu/docker-compose.override.yml` 안에 작성하면 되고, openvidu platform과 생명주기를 같이 합니다.

<br>

다음 요구 사항이 지켜져야 합니다.
- network_mode를 host로 해야 합니다.
- Application은 반드시 https가 아닌 http이어야 합니다.
- http port는 5442이어야 한다. Openvidu platform의 nginx 설정에서 해당 port로 되어 있습니다.
- openvidu platform URL은 `http://localhost:5443`입니다.
- openVidu secret은 환경변수 ${OPENVIDU_SECRET}로 활용 가능합니다.

<br>

### application 서버 구축
우선, Docker와 Docker Compose가 설치되어 있어야 합니다.

docker buildkit을 위해 `docker/dockerfile:1` 이미지를 미리 pull 합니다.
```sh
docker pull docker/dockerfile:1
```

<br>

`~/camfia` directory를 생성 및 이동합니다.
```sh
mkdir ~/camfia
cd ~/camfia
```

<br>

project의 다음 파일을 복사합니다.
- [docker-compose.yml](./docker-compose.yml)
- [init-letsencrypt.sh](./init-letsencrypt.sh)
- [nginx/default.conf.template](nginx/default.conf.template)

<br>

이때, docker compose와 certbot을 실행하기 위해서는 `.env` 파일이 필요합니다. `.env`의 내용은 다음과 같습니다.
```env
REGISTRY=camfia.jfrog.io
APP_DOMAIN=my-app.com
LETSENCRYPT_EMAIL=example@email.com

# mysql
MYSQL_USER=myuser
MYSQL_PASSWORD=mypassword
MYSQL_ROOT_PASSWORD=myrootpassword
MYSQL_DATABASE=mydb

# backend
GOOGLE_CLIENT_ID=1q2w3e4r-1q2w3e4r.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=4r3e2w1q4r3e2w1q
SPRING_DATASOURCE_USERNAME=${MYSQL_USER}
SPRING_DATASOURCE_PASSWORD=${MYSQL_PASSWORD}
JWT_SECRET=1q2w3e4r
APP_BASE_URL=https://${APP_DOMAIN}/
OPENVIDU_URL=https://my-openvidu-server.com/
OPENVIDU_SECRET=MY_OPNEVIDU_SECRET

# mysql, backend, nginx
TZ=Asia/Seoul
```

<br>

[init-letsencrypt.sh](./init-letsencrypt.sh)을 실행시켜 certbot에 의해 SSL/TLS 인증서를 생성하도록 해야 합니다.
```sh
sudo ./init-letsencrypt.sh
```

<br>

docker compose를 통해 application을 실행합니다.
```sh
docker-compose up -d
```

<br>

### 배포시 주의 사항
docker volume인 redis-data와 mysql-data는 각각 redis container와 mysql(mariadb) container의 데이터 저장 volume이므로 각별히 주의해야 합니다.

<br>

## github actions를 통한 자동 배포
[auto-deploy.yml](.github/workflows/auto-deploy.yml)에 의해 자동으로 배포가 이루어 집니다.

### github secrets 설정
`Settings > Secrets`에서 다음 항목에 대한 secrets 등록이 필요합니다.
```sh
DEV_BASE_URL : https://my-app-dev.com
DEV_HOST : my-app-dev.com
JIRA_BASE_URL : https://my-app.atlassian.net/
PROD_BASE_URL : https://my-app.com
PROD_HOST : my-app.com
REGISTRY : camfia.jfrog.io
REGISTRY_PASSWORD : registrypass
REGISTRY_USERNAME : registryuser
SSH_PORT : 22
SSH_PRIVATE_KEY : -----BEGIN OPENSSH PRIVATE KEY----- ~~
SSH_USERNAME : sshuser
```
