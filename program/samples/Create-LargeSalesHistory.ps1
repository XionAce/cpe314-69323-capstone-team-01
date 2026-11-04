param(
    [ValidateRange(1, 1000000)][int]$Count = 100000,
    [string]$OutputPath = (Join-Path $env:TEMP 'campus-large-sales.json')
)
# Generates clearly fictional sales for demonstrating a real background import.
$salesWriter = [System.IO.StreamWriter]::new($OutputPath, $false, [System.Text.UTF8Encoding]::new($false))
try {
    $salesWriter.WriteLine('[')
    for ($recordNumber = 1; $recordNumber -le $Count; $recordNumber++) {
        if ($recordNumber -gt 1) { $salesWriter.WriteLine(',') }
        $salesWriter.Write('{"sourceRecordId":"large-demo-' + $recordNumber + '","categoryPath":["All finds","Textbooks"],"price":250.00,"currency":"PHP","soldAt":"2026-10-20T08:00:00Z"}')
    }
    $salesWriter.WriteLine(']')
} finally { $salesWriter.Dispose() }
Write-Output "Created $Count fictional sales at $OutputPath"
