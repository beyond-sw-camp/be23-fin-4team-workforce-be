from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate  # ← 수정
from app.config import settings

llm = ChatOpenAI(
    model="gpt-4o-mini",
    temperature=0,
    openai_api_key=settings.openai_api_key
)

HR_PROMPT = ChatPromptTemplate.from_template("""
당신은 회사의 HR 정책 전문 AI 어시스턴트입니다.
아래 제공된 회사 정책 문서를 바탕으로 직원의 질문에 답변하세요.

[회사 정책 문서]
{context}

[직원 질문]
{question}

[답변 규칙]
- 제공된 문서 내용을 기반으로만 답변하세요
- 문서에 없는 내용은 "HR 담당자에게 문의해주세요" 라고 안내하세요
- 친절하고 명확하게 답변하세요

답변:
""")

def generate_answer(context: str, question: str) -> str:
    chain = HR_PROMPT | llm
    response = chain.invoke({
        "context": context,
        "question": question
    })
    return response.content