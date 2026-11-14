# Configura los secretos de la API en ESTE equipo (fuera del repositorio):
#   - Jwt:Clave              -> clave aleatoria de 64 bytes para firmar los JWT
#   - AdminInicial:Correo    -> correo del administrador inicial
#   - AdminInicial:Contrasena
# Los guarda con "dotnet user-secrets" en %APPDATA%\Microsoft\UserSecrets (no en el proyecto).
# Ademas crea postman\Bitacora.local.postman_environment.json (excluido por .gitignore).
#
# Uso (desde la carpeta BitacoraApi):
#   powershell -ExecutionPolicy Bypass -File .\configurar_seguridad.ps1

param([string]$AdminCorreo = "admin@bitacora.com")

function Detener($m) { Write-Host "ERROR: $m" -ForegroundColor Red; exit 1 }

if (-not (Test-Path ".\Bitacora.sln")) { Detener "Ejecuta el script desde la carpeta BitacoraApi." }

# 1. Clave de firma aleatoria (nunca escrita en el codigo)
$bytes = New-Object byte[] 64
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$clave = [Convert]::ToBase64String($bytes)

# 2. Contrasena del administrador (no se muestra en pantalla)
do {
    $segura = Read-Host "Escribe la contrasena para $AdminCorreo (minimo 8, con letras y numeros)" -AsSecureString
    $admin = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura))
    $valida = ($admin.Length -ge 8) -and ($admin -match "[A-Za-z]") -and ($admin -match "\d")
    if (-not $valida) { Write-Host "La contrasena no cumple la regla. Intenta de nuevo." -ForegroundColor Yellow }
} until ($valida)

Write-Host "Guardando secretos con dotnet user-secrets..." -ForegroundColor Cyan
dotnet user-secrets set "Jwt:Clave" "$clave" --project src/Bitacora.Api | Out-Null
if ($LASTEXITCODE -ne 0) { Detener "No se pudo guardar Jwt:Clave." }
dotnet user-secrets set "AdminInicial:Correo" "$AdminCorreo" --project src/Bitacora.Api | Out-Null
dotnet user-secrets set "AdminInicial:Contrasena" "$admin" --project src/Bitacora.Api | Out-Null
if ($LASTEXITCODE -ne 0) { Detener "No se pudieron guardar las credenciales del administrador." }

# 3. Entorno local de Postman con los valores (archivo ignorado por git)
$entorno = Get-Content "postman\Bitacora.postman_environment.json" -Raw | ConvertFrom-Json
foreach ($v in $entorno.values) {
    if ($v.key -eq "adminCorreo")     { $v.value = $AdminCorreo }
    if ($v.key -eq "adminContrasena") { $v.value = $admin }
    if ($v.key -eq "jwtClave")        { $v.value = $clave }
}
$entorno.name = "Bitacora - Local (este equipo)"
$entorno.id = [guid]::NewGuid().ToString()
$entorno | ConvertTo-Json -Depth 6 | Set-Content "postman\Bitacora.local.postman_environment.json" -Encoding UTF8

# 4. Certificado HTTPS de desarrollo (Windows pedira confirmacion)
dotnet dev-certs https --trust | Out-Null

Write-Host ""
Write-Host "LISTO." -ForegroundColor Green
Write-Host " - Secretos guardados fuera del repositorio (dotnet user-secrets list --project src/Bitacora.Api)."
Write-Host " - Entorno de Postman para importar: postman\Bitacora.local.postman_environment.json"
Write-Host "Siguiente: dotnet ef database update --project src/Bitacora.Infrastructure --startup-project src/Bitacora.Api"
