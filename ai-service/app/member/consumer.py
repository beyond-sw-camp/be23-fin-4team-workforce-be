from kafka import KafkaConsumer
from sqlalchemy.orm import Session
from app.database import SessionLocal
from app.member.model import MemberCache
from app.member.schema import MemberSavedEvent, MemberDeletedEvent
from app.config import settings
import json
import logging
import threading

logger = logging.getLogger(__name__)

def get_db() -> Session:
    return SessionLocal()

def handle_member_saved(data: dict, db: Session):
    try:
        event = MemberSavedEvent(**data)

        # 조직명 추출
        organization_name = None
        if event.organizationList:
            organization_name = event.organizationList[0].name

        # 직책명 추출
        job_title_name = None
        if event.titleName:
            job_title_name = event.titleName[0]

        # 이미 있으면 업데이트, 없으면 생성
        member = db.query(MemberCache)\
            .filter(MemberCache.member_id == event.memberId)\
            .first()

        if member:
            member.company_id      = event.companyId
            member.name            = event.name
            member.email           = event.email
            member.sabun           = event.sabun
            member.join_date       = event.joinDate
            member.member_status   = event.memberStatus
            member.organization_name = organization_name
            member.job_grade_name  = event.jobGradeName
            member.job_title_name  = job_title_name
            member.esg_score       = event.esgScore
        else:
            member = MemberCache(
                member_id        = event.memberId,
                company_id       = event.companyId,
                name             = event.name,
                email            = event.email,
                sabun            = event.sabun,
                join_date        = event.joinDate,
                member_status    = event.memberStatus,
                organization_name = organization_name,
                job_grade_name   = event.jobGradeName,
                job_title_name   = job_title_name,
                esg_score        = event.esgScore
            )
            db.add(member)

        db.commit()
        logger.info(f"직원 정보 캐싱 완료: {event.memberId}")

    except Exception as e:
        db.rollback()
        logger.error(f"member-saved 처리 실패: {str(e)}")

def handle_member_deleted(data: dict, db: Session):
    try:
        event = MemberDeletedEvent(**data)

        db.query(MemberCache)\
            .filter(MemberCache.member_id == event.memberId)\
            .delete()
        db.commit()
        logger.info(f"직원 캐시 삭제 완료: {event.memberId}")

    except Exception as e:
        db.rollback()
        logger.error(f"member-deleted 처리 실패: {str(e)}")

def start_consumer():
    print("===== Kafka 연결 시도 =====")
    try:
        consumer = KafkaConsumer(
            "member-saved",
            "member-deleted",
            bootstrap_servers=settings.kafka_bootstrap_servers,
            group_id="ai-service-member-group",
            value_deserializer=lambda v: json.loads(v.decode("utf-8")),
            auto_offset_reset="earliest",
            enable_auto_commit=True
        )
        print("===== Kafka 연결 성공 =====")
    except Exception as e:
        print(f"===== Kafka 연결 실패: {e} =====")
        return

    for message in consumer:
        print(f"===== 메시지 수신: topic={message.topic}, value={message.value} =====")
        db = get_db()
        try:
            topic = message.topic
            data = message.value

            if topic == "member-saved":
                handle_member_saved(data, db)
            elif topic == "member-deleted":
                handle_member_deleted(data, db)

        except Exception as e:
            print(f"===== 메시지 처리 실패: {e} =====")
            logger.error(f"Kafka 메시지 처리 실패: {str(e)}")
        finally:
            db.close()

def start_consumer_thread():
    def run():
        try:
            start_consumer()
        except Exception as e:
            print(f"===== Consumer 스레드 에러로 종료: {e} =====")
            import traceback
            traceback.print_exc()

    thread = threading.Thread(
        target=run,
        daemon=True,
        name="kafka-member-consumer"
    )
    thread.start()
    print(f"===== Kafka consumer 스레드 시작 완료 =====")