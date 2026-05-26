import { QuestionDetailView } from "@/features/questions/components/question-detail-view";

export default async function QuestionDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  return <QuestionDetailView questionId={Number(id)} />;
}
