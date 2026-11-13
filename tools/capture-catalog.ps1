<# Save an actual public response for the instructor's parser-contract follow-up.
   Run from PowerShell on a working connection. Existing captures are preserved. #>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$captureFolder = Join-Path $repoRoot 'program\src\test\resources\api'
$responsePath = Join-Path $captureFolder 'fakestore-products.json'
$metadataPath = Join-Path $captureFolder 'fakestore-products-capture.json'
$endpoint = 'https://fakestoreapi.com/products'

if ((Test-Path -LiteralPath $responsePath) -or (Test-Path -LiteralPath $metadataPath)) {
    throw 'A capture already exists. Review it before replacing any saved evidence.'
}

# Fetch and validate before creating files. A failed request saves no pretend response.
$reply = Invoke-WebRequest -Uri $endpoint -Headers @{ Accept = 'application/json' } `
    -UseBasicParsing -TimeoutSec 20
if ([int]$reply.StatusCode -ne 200) { throw 'The public endpoint did not return HTTP 200.' }
$rawJson = [string]$reply.Content
if (-not $rawJson.TrimStart().StartsWith('[')) { throw 'Expected a JSON product array.' }
$decoded = ConvertFrom-Json -InputObject $rawJson
$products = @($decoded)
if ($products.Count -eq 0) { throw 'The catalog is empty; retain evidence only after reviewing the response.' }
foreach ($product in $products) {
    if ($null -eq $product.id -or $null -eq $product.price -or
        [string]::IsNullOrWhiteSpace([string]$product.title) -or
        [string]::IsNullOrWhiteSpace([string]$product.category)) {
        throw 'The response lacks product fields required by the Java parser.'
    }
}

New-Item -ItemType Directory -Path $captureFolder -Force | Out-Null
$encoding = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllText($responsePath, $rawJson, $encoding)
$metadata = [ordered]@{
    endpoint = $endpoint
    capturedAtUtc = [DateTime]::UtcNow.ToString('o')
    httpStatus = [int]$reply.StatusCode
    productCount = $products.Count
    sha256 = (Get-FileHash -LiteralPath $responsePath -Algorithm SHA256).Hash.ToLowerInvariant()
}
[IO.File]::WriteAllText($metadataPath, ($metadata | ConvertTo-Json), $encoding)
Write-Output ('Captured HTTP 200 with ' + $products.Count + ' products: ' + $responsePath)
Write-Output 'Next: verify this actual fixture with ProductCatalogParser and add its contract test.'
Write-Output 'This capture does not replace the live GUI network on/off demonstration.'
