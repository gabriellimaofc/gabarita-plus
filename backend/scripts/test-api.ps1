param(
    [string]$BaseUrl = "http://localhost:8080/api",
    [string]$UsernameOrEmail = "user@gabaritaplus.com",
    [string]$Password = "User@123"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "-> $Message" -ForegroundColor Cyan
}

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

Write-Step "Health"
$health = Invoke-RestMethod -Method Get -Uri "$BaseUrl/actuator/health"
$health | ConvertTo-Json -Depth 5

Write-Step "Login"
$loginBody = @{
    usernameOrEmail = $UsernameOrEmail
    password = $Password
} | ConvertTo-Json

$login = Invoke-RestMethod `
    -Method Post `
    -Uri "$BaseUrl/auth/login" `
    -ContentType "application/json" `
    -Body $loginBody

$accessToken = $login.data.accessToken
$refreshToken = $login.data.refreshToken
$headers = @{ Authorization = "Bearer $accessToken" }

[pscustomobject]@{
    Success = $login.success
    Message = $login.message
    HasAccessToken = [bool]$accessToken
    HasRefreshToken = [bool]$refreshToken
    UserId = $login.data.user.id
    UserEmail = $login.data.user.email
} | ConvertTo-Json -Depth 5

Write-Step "Dashboard"
$dashboard = Invoke-RestMethod -Method Get -Uri "$BaseUrl/dashboard" -Headers $headers
$dashboard | ConvertTo-Json -Depth 5

Write-Step "Perfil"
$profile = Invoke-RestMethod -Method Get -Uri "$BaseUrl/users/me" -Headers $headers
$profile | ConvertTo-Json -Depth 5

Write-Step "Questoes"
$questions = Invoke-RestMethod -Method Get -Uri "$BaseUrl/questions?page=0&size=5" -Headers $headers
$questions | ConvertTo-Json -Depth 5

Assert-True ($questions.data.Count -ge 2) "O smoke test precisa de pelo menos 2 questoes cadastradas."

$firstQuestion = $questions.data[0]
Assert-True (-not ($firstQuestion.PSObject.Properties.Name -contains "correctAlternative")) "A questao nao deveria expor correctAlternative antes da resposta."
Assert-True (-not ($firstQuestion.PSObject.Properties.Name -contains "explanation")) "A questao nao deveria expor explanation antes da resposta."

Write-Step "Criar simulado"
$questionIds = @($questions.data[0].id, $questions.data[1].id)
$createExamBody = @{
    title = "Smoke test $(Get-Date -Format 'yyyyMMddHHmmss')"
    durationMinutes = 60
    questionIds = $questionIds
} | ConvertTo-Json

$createdExam = Invoke-RestMethod `
    -Method Post `
    -Uri "$BaseUrl/mock-exams" `
    -Headers $headers `
    -ContentType "application/json" `
    -Body $createExamBody

$mockExamId = $createdExam.data.id
[pscustomobject]@{
    MockExamId = $mockExamId
    QuestionCount = $createdExam.data.questionCount
} | ConvertTo-Json -Depth 5

Write-Step "Responder questao do simulado"
$answerExamBody = @{
    questionId = $questionIds[0]
    chosenAlternative = "A"
    timeSpentSeconds = 30
} | ConvertTo-Json

$answeredExam = Invoke-RestMethod `
    -Method Post `
    -Uri "$BaseUrl/mock-exams/$mockExamId/answers" `
    -Headers $headers `
    -ContentType "application/json" `
    -Body $answerExamBody

[pscustomobject]@{
    AnsweredCount = $answeredExam.data.answeredCount
    UnansweredCount = $answeredExam.data.unansweredCount
} | ConvertTo-Json -Depth 5

Write-Step "Questoes do simulado antes da finalizacao"
$examQuestions = Invoke-RestMethod `
    -Method Get `
    -Uri "$BaseUrl/mock-exams/$mockExamId/questions" `
    -Headers $headers

$examQuestion = $examQuestions.data[0]
Assert-True (-not ($examQuestion.PSObject.Properties.Name -contains "correctAlternative")) "O simulado nao deveria expor correctAlternative antes de finalizar."
Assert-True (-not ($examQuestion.PSObject.Properties.Name -contains "explanation")) "O simulado nao deveria expor explanation antes de finalizar."

[pscustomobject]@{
    FirstQuestionId = $examQuestion.questionId
    ChosenAlternative = $examQuestion.chosenAlternative
    HasCorrectAlternative = ($examQuestion.PSObject.Properties.Name -contains "correctAlternative")
} | ConvertTo-Json -Depth 5

Write-Step "Finalizar simulado"
$finishExamBody = @{
    timeSpentSeconds = 1800
} | ConvertTo-Json

$finishedExam = Invoke-RestMethod `
    -Method Post `
    -Uri "$BaseUrl/mock-exams/$mockExamId/finish" `
    -Headers $headers `
    -ContentType "application/json" `
    -Body $finishExamBody

[pscustomobject]@{
    FinalScore = $finishedExam.data.finalScore
    CorrectAnswers = $finishedExam.data.correctAnswers
    IncorrectAnswers = $finishedExam.data.incorrectAnswers
    Finished = $finishedExam.data.finished
} | ConvertTo-Json -Depth 5

Write-Step "Resultado do simulado"
$resultExam = Invoke-RestMethod `
    -Method Get `
    -Uri "$BaseUrl/mock-exams/$mockExamId/result" `
    -Headers $headers

[pscustomobject]@{
    QuestionCount = $resultExam.data.questionCount
    SubjectBuckets = $resultExam.data.performanceBySubject.Count
    RevealedCorrectAlternative = [bool]$resultExam.data.questions[0].correctAlternative
} | ConvertTo-Json -Depth 5

Write-Step "Refresh token"
$refreshBody = @{ refreshToken = $refreshToken } | ConvertTo-Json
$refresh = Invoke-RestMethod `
    -Method Post `
    -Uri "$BaseUrl/auth/refresh" `
    -ContentType "application/json" `
    -Body $refreshBody

[pscustomobject]@{
    Success = $refresh.success
    Message = $refresh.message
    HasAccessToken = [bool]$refresh.data.accessToken
    ReturnedSameRefreshToken = ($refresh.data.refreshToken -eq $refreshToken)
    UserId = $refresh.data.user.id
} | ConvertTo-Json -Depth 5

Write-Host ""
Write-Host "Todos os testes principais passaram." -ForegroundColor Green
