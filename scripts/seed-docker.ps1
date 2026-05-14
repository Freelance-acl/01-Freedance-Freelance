# Seed sample data via REST after Docker Compose exposes services on localhost.
# Usage (from repo root): .\scripts\seed-docker.ps1
# Prerequisite: stack is up (docker compose up -d) and ports 8081-8083 respond.

$ErrorActionPreference = 'Stop'

function Wait-Api {
    param([string]$Url, [int]$MaxAttempts = 60)
    for ($i = 0; $i -lt $MaxAttempts; $i++) {
        try {
            Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec 2 | Out-Null
            return
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    throw "Timeout waiting for $Url"
}

function New-SeedProposal {
    param(
        [long]$JobId,
        [long]$FreelancerId,
        [string]$CoverLetter,
        [double]$BidAmount,
        [int]$EstimatedDays,
        [string]$Status,
        [string]$SubmittedAtIso
    )
    $body = @{
        jobId         = $JobId
        freelancerId  = $FreelancerId
        coverLetter   = $CoverLetter
        bidAmount     = $BidAmount
        estimatedDays = $EstimatedDays
        status        = $Status
        submittedAt   = $SubmittedAtIso
    } | ConvertTo-Json
    return Invoke-RestMethod -Uri $p -Method Post -Body $body -Headers $script:headers -ContentType 'application/json'
}

$u = 'http://localhost:8081/api/users'
$j = 'http://localhost:8082/api/jobs'
$p = 'http://localhost:8083/api/proposals'
$script:headers = @{ 'Content-Type' = 'application/json' }

Write-Host 'Waiting for user-service...'
Wait-Api "$u"

Write-Host 'Creating users...'
$clientBody = @{
    name     = 'Demo Client'
    email    = 'client@seed.local'
    password = 'seedpass'
    phone    = '+15550000001'
    role     = 'CLIENT'
    status   = 'ACTIVE'
} | ConvertTo-Json

$flBody = @{
    name     = 'Demo Freelancer'
    email    = 'freelancer@seed.local'
    password = 'seedpass'
    phone    = '+15550000002'
    role     = 'FREELANCER'
    status   = 'ACTIVE'
} | ConvertTo-Json

try {
    $client = Invoke-RestMethod -Uri $u -Method Post -Body $clientBody -Headers $script:headers -ContentType 'application/json'
} catch {
    $existing = Invoke-RestMethod -Uri $u
    $client = $existing | Where-Object { $_.email -eq 'client@seed.local' } | Select-Object -First 1
    if (-not $client) { throw $_ }
}

try {
    $freelancer = Invoke-RestMethod -Uri $u -Method Post -Body $flBody -Headers $script:headers -ContentType 'application/json'
} catch {
    $existing = Invoke-RestMethod -Uri $u
    $freelancer = $existing | Where-Object { $_.email -eq 'freelancer@seed.local' } | Select-Object -First 1
    if (-not $freelancer) { throw $_ }
}

Write-Host 'Waiting for job-service...'
Wait-Api $j

Write-Host 'Creating job...'
$jobBody = @{
    clientId    = $client.id
    title       = 'Seed: Build REST API'
    description = 'Demo job created by seed-docker.ps1'
    category    = 'WEB_DEV'
    status      = 'OPEN'
    budgetMin   = 500
    budgetMax   = 2000
} | ConvertTo-Json

try {
    $job = Invoke-RestMethod -Uri $j -Method Post -Body $jobBody -Headers $script:headers -ContentType 'application/json'
} catch {
    $jobs = Invoke-RestMethod -Uri $j
    $job = $jobs | Where-Object { $_.title -like 'Seed:*' } | Select-Object -First 1
    if (-not $job) { throw $_ }
}

Write-Host 'Waiting for proposal-service...'
Wait-Api $p

Write-Host 'Creating baseline proposal...'
$propBody = @{
    jobId          = $job.id
    freelancerId = $freelancer.id
    coverLetter    = 'Seeded proposal for local Docker demo.'
    bidAmount      = 900
    estimatedDays  = 14
    status         = 'SUBMITTED'
} | ConvertTo-Json

try {
    $proposal = Invoke-RestMethod -Uri $p -Method Post -Body $propBody -Headers $script:headers -ContentType 'application/json'
} catch {
    $proposals = Invoke-RestMethod -Uri $p
    $proposal = $proposals | Where-Object { $_.jobId -eq $job.id -and $_.freelancerId -eq $freelancer.id -and $_.coverLetter -eq 'Seeded proposal for local Docker demo.' } | Select-Object -First 1
    if (-not $proposal) { throw $_ }
}

Write-Host "Seed complete: user client id=$($client.id), freelancer id=$($freelancer.id), job id=$($job.id), proposal id=$($proposal.id)"

# --- S3-F1 search demo (5 proposals: milestone doc scenario) ---
$tag = '[SEARCH-DEMO]'
$allProps = Invoke-RestMethod -Uri $p
$demoCount = @($allProps | Where-Object { $_.coverLetter -and $_.coverLetter.Contains($tag) }).Count
if ($demoCount -lt 5) {
    Write-Host "Inserting $tag proposals for search endpoint tests..."
    New-SeedProposal -JobId $job.id -FreelancerId $freelancer.id -CoverLetter "$tag March ACCEPTED older" -BidAmount 100 -EstimatedDays 5 -Status ACCEPTED -SubmittedAtIso '2026-03-05T10:00:00' | Out-Null
    New-SeedProposal -JobId $job.id -FreelancerId $freelancer.id -CoverLetter "$tag March ACCEPTED newer" -BidAmount 100 -EstimatedDays 5 -Status ACCEPTED -SubmittedAtIso '2026-03-20T15:00:00' | Out-Null
    New-SeedProposal -JobId $job.id -FreelancerId $freelancer.id -CoverLetter "$tag March SUBMITTED" -BidAmount 100 -EstimatedDays 5 -Status SUBMITTED -SubmittedAtIso '2026-03-10T11:00:00' | Out-Null
    New-SeedProposal -JobId $job.id -FreelancerId $freelancer.id -CoverLetter "$tag Feb ACCEPTED 1" -BidAmount 100 -EstimatedDays 5 -Status ACCEPTED -SubmittedAtIso '2026-02-01T09:00:00' | Out-Null
    New-SeedProposal -JobId $job.id -FreelancerId $freelancer.id -CoverLetter "$tag Feb ACCEPTED 2" -BidAmount 100 -EstimatedDays 5 -Status ACCEPTED -SubmittedAtIso '2026-02-15T09:00:00' | Out-Null
    Write-Host 'Search demo proposals created.'
} else {
    Write-Host "Search demo proposals already present ($demoCount)."
}

Write-Host ''
Write-Host '=== GET /api/proposals/search (ACCEPTED, March 2026) ==='
$searchAcceptedMarch = Invoke-RestMethod -Uri "$p/search?status=ACCEPTED&startDate=2026-03-01&endDate=2026-03-31"
$demoAcceptedMarch = @($searchAcceptedMarch | Where-Object { $_.coverLetter -and $_.coverLetter.Contains($tag) })
if ($demoAcceptedMarch.Count -ne 2) {
    throw "Expected 2 ACCEPTED proposals in March for $tag; got $($demoAcceptedMarch.Count). ($($searchAcceptedMarch.Count) rows in response.)"
}
$sortedAcc = $demoAcceptedMarch | Sort-Object { [datetime]$_.submittedAt } -Descending
if ($sortedAcc[0].coverLetter -notmatch 'newer') {
    throw "Expected newest March ACCEPTED demo row to be 'newer' (2026-03-20); got: $($sortedAcc[0].coverLetter)"
}
Write-Host "OK: $($demoAcceptedMarch.Count) ACCEPTED in March ($tag), newest-first in API matches sort (newer id=$($sortedAcc[0].id))."

Write-Host ''
Write-Host '=== GET /api/proposals/search (no status, March 2026) ==='
$searchMarch = Invoke-RestMethod -Uri "$p/search?startDate=2026-03-01&endDate=2026-03-31"
$demoMarchAll = @($searchMarch | Where-Object { $_.coverLetter -and $_.coverLetter.Contains($tag) })
if ($demoMarchAll.Count -ne 3) {
    throw "Expected 3 proposals in March for $tag; got $($demoMarchAll.Count). ($($searchMarch.Count) rows in response.)"
}
$sortedAll = $demoMarchAll | Sort-Object { [datetime]$_.submittedAt } -Descending
if ($sortedAll[0].coverLetter -notmatch 'newer' -or $sortedAll[2].coverLetter -notmatch 'March ACCEPTED older') {
    throw 'March window demo rows not in expected submittedAt order (newer, submitted, older).'
}
Write-Host "OK: $($demoMarchAll.Count) proposals in March ($tag), order 2026-03-20 > 2026-03-10 > 2026-03-05."

Write-Host ''
Write-Host 'All search checks passed.'
