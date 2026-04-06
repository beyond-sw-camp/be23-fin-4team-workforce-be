from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from app.config import settings

llm = ChatOpenAI(
    model="gpt-4o-mini",
    temperature=0,
    openai_api_key=settings.openai_api_key
)

HR_PROMPT = ChatPromptTemplate.from_template("""
당신은 회사의 HR 정책 전문 AI 어시스턴트입니다.
아래 제공된 회사 정책 문서를 바탕으로 직원의 질문에 답변하세요.

{member_context}

[회사 정책 문서]
{context}

[이전 대화 이력]
{conversation_history}

[직원 질문]
{question}

[답변 규칙]
- 제공된 문서 내용을 기반으로만 답변하세요
- 직원 정보를 참고하여 개인화된 답변을 제공하세요
- 예를 들어 입사일 기준 연차 계산, ESG 점수 기반 등급 안내 등이 가능합니다
- 이전 대화 이력을 참고하여 연속적인 질문에 답변하세요
- 문서에 없는 내용은 "HR 담당자에게 문의해주세요" 라고 안내하세요
- 친절하고 명확하게 답변하세요

답변:
""")

def generate_answer(
        context: str,
        question: str,
        conversation_history: str = "",
        member_context: str = "") -> str:
    chain = HR_PROMPT | llm
    response = chain.invoke({
        "member_context": member_context,
        "context": context,
        "question": question,
        "conversation_history": conversation_history
    })
    return response.content