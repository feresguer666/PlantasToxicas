#!/usr/bin/env bash
set -euo pipefail

if [ ! -f "app/build.gradle.kts" ] || [ ! -d "app/src/main/java" ]; then
  echo "ERROR: ejecuta este script desde la raíz del proyecto PlantasToxicas" >&2
  exit 1
fi

python3 - <<'PY'
from pathlib import Path
import shutil
from datetime import datetime

path = Path("app/src/main/java/com/toxicplants/database/ui/screens/CameraIdentifyScreen.kt")
if not path.exists():
    raise SystemExit("ERROR: no existe CameraIdentifyScreen.kt")

original = path.read_text(encoding="utf-8")
text = original
stamp = datetime.now().strftime("%Y%m%d_%H%M%S")

# 1) Launcher de cámara directa
if "cameraLauncher" not in text:
    marker = '''    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    selectedBitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) { }
        }
    }
'''
    insert = marker + '''
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            selectedImageUri = null
            selectedBitmap = it
            identificationResults = emptyList()
        }
    }
'''
    if marker not in text:
        raise SystemExit("ERROR: no encuentro imagePickerLauncher para insertar cameraLauncher")
    text = text.replace(marker, insert, 1)

# 2) Función openCamera
if "fun openCamera()" not in text:
    marker = '''    fun selectImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            imagePickerLauncher.launch("image/*")
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }
'''
    insert = marker + '''
    fun openCamera() {
        cameraLauncher.launch(null)
    }
'''
    if marker not in text:
        raise SystemExit("ERROR: no encuentro selectImage() para insertar openCamera()")
    text = text.replace(marker, insert, 1)

# 3) Limpiar resultados cuando se selecciona imagen de galería también
text = text.replace(
    '''                    selectedBitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) { }
''',
    '''                    selectedBitmap = BitmapFactory.decodeStream(stream)
                    identificationResults = emptyList()
                }
            } catch (e: Exception) { }
''',
    1
)

# 4) Sustituir fila de botones por 3 botones: Galería, Cámara, Identificar
old = '''            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { selectImage() }, modifier = Modifier.weight(1f)) {
                    Text("🖼️ Galería", fontSize = 14.sp)
                }
                Button(onClick = { identifyPlant() }, modifier = Modifier.weight(1f), enabled = !isLoading && selectedBitmap != null, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    else Text("🌿 Identificar", fontSize = 14.sp)
                }
            }
'''
new = '''            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { selectImage() }, modifier = Modifier.weight(1f)) {
                    Text("🖼️ Galería", fontSize = 12.sp)
                }
                OutlinedButton(onClick = { openCamera() }, modifier = Modifier.weight(1f)) {
                    Text("📷 Cámara", fontSize = 12.sp)
                }
                Button(onClick = { identifyPlant() }, modifier = Modifier.weight(1f), enabled = !isLoading && selectedBitmap != null, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    else Text("🌿 Identificar", fontSize = 12.sp)
                }
            }
'''
if old not in text:
    raise SystemExit("ERROR: no encuentro la fila actual de Galería/Identificar para cambiarla")
text = text.replace(old, new, 1)

# 5) Texto informativo
text = text.replace(
    "🔍 Selecciona una foto y presiona identificar",
    "🔍 Elige galería o cámara y presiona identificar"
)

if text != original:
    bak = path.with_suffix(path.suffix + f".bak_camara_identificar_{stamp}")
    shutil.copy2(path, bak)
    path.write_text(text, encoding="utf-8")
    print(f"backup: {bak}")
    print("OK: añadido botón Cámara en Identificar Planta")
else:
    print("Sin cambios: ya estaba aplicado")
PY
