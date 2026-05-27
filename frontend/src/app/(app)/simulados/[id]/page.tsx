import { MockExamDetailView } from "@/features/mock-exams/components/mock-exam-detail-view";

export default async function MockExamDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  return <MockExamDetailView mockExamId={Number(id)} />;
}
