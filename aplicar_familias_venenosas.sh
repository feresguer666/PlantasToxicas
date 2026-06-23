#!/usr/bin/env bash
set -euo pipefail

# Ejecuta este script desde la raíz del proyecto PlantasToxicas.
# Busca new_files en la raíz; si no, también acepta familias_venenosas_patch/new_files
# para quienes descomprimieron el ZIP anterior.

if [ ! -f "app/build.gradle.kts" ] || [ ! -d "app/src/main/java" ]; then
  echo "ERROR: ejecuta este script desde la raíz de tu proyecto PlantasToxicas." >&2
  echo "Ejemplo: cd ~/AndroidStudioProjects/PlantasToxicas && bash aplicar_familias_venenosas.sh" >&2
  exit 1
fi

if [ -d "new_files" ]; then
  NEW_FILES_DIR="new_files"
elif [ -d "familias_venenosas_patch/new_files" ]; then
  NEW_FILES_DIR="familias_venenosas_patch/new_files"
else
  echo "ERROR: no encuentro new_files." >&2
  echo "Solución: descomprime el ZIP dentro de la raíz de PlantasToxicas." >&2
  echo "Debe quedar así: PlantasToxicas/new_files/app/..." >&2
  exit 1
fi

echo "==> Copiando archivos nuevos desde $NEW_FILES_DIR ..."
cp -R "$NEW_FILES_DIR/app" .

python3 - <<'PY'
from pathlib import Path
import re
import shutil
import sys
from datetime import datetime

ROOT = Path.cwd()
STAMP = datetime.now().strftime("%Y%m%d_%H%M%S")

def backup(path: Path):
    if not path.exists():
        raise SystemExit(f"ERROR: no existe {path}")
    bak = path.with_suffix(path.suffix + f".bak_familias_venenosas_{STAMP}")
    shutil.copy2(path, bak)
    print(f"  backup: {bak}")


def write_if_changed(path: Path, text: str, original: str):
    if text != original:
        backup(path)
        path.write_text(text, encoding="utf-8")
        print(f"  parcheado: {path}")
    else:
        print(f"  sin cambios: {path}")


def patch_plant_database():
    path = ROOT / "app/src/main/java/com/toxicplants/database/PlantDatabase.kt"
    original = path.read_text(encoding="utf-8")
    text = original

    already = "PoisonousFamilyGenusEntity::class" in text
    old_version = None
    new_version = None
    migration_name = None

    if not already:
        m = re.search(r"(entities\s*=\s*\[)(.*?)(\])", text, flags=re.S)
        if not m:
            raise SystemExit("ERROR: no encuentro entities = [...] en PlantDatabase.kt")
        content = m.group(2)
        if "\n" in content:
            stripped = content.rstrip()
            if stripped.strip() and not stripped.rstrip().endswith(','):
                stripped += ','
            new_content = stripped + "\n        PoisonousFamilyGenusEntity::class\n    "
        else:
            new_content = content.rstrip() + ", PoisonousFamilyGenusEntity::class"
        text = text[:m.start(2)] + new_content + text[m.end(2):]

        vm = re.search(r"version\s*=\s*(\d+)", text)
        if not vm:
            raise SystemExit("ERROR: no encuentro version = N en PlantDatabase.kt")
        old_version = int(vm.group(1))
        new_version = old_version + 1
        migration_name = f"MIGRATION_{old_version}_{new_version}"
        text = text[:vm.start(1)] + str(new_version) + text[vm.end(1):]

    if "abstract fun poisonousFamilyDao()" not in text:
        anchor = re.search(r"(\n\s*abstract fun toxicCalendarDao\(\): ToxicCalendarDao\s*\n)", text)
        if anchor:
            text = text[:anchor.end(1)] + "    abstract fun poisonousFamilyDao(): PoisonousFamilyDao\n" + text[anchor.end(1):]
        else:
            anchor2 = re.search(r"(\n\s*companion object\s*\{)", text)
            if not anchor2:
                raise SystemExit("ERROR: no encuentro dónde insertar poisonousFamilyDao()")
            text = text[:anchor2.start(1)] + "\n    abstract fun poisonousFamilyDao(): PoisonousFamilyDao\n" + text[anchor2.start(1):]

    if migration_name and migration_name not in text:
        migration_block = f'''

        /**
         * v{old_version} → v{new_version}: crea la tabla editable de Familias venenosas.
         * Cada fila representa un género dentro de una familia venenosa.
         */
        val {migration_name} = object : Migration({old_version}, {new_version}) {{
            override fun migrate(db: SupportSQLiteDatabase) {{
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `poisonous_family_genera` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `familyName` TEXT NOT NULL,
                        `genusName` TEXT NOT NULL,
                        `genusSpeciesCount` INTEGER NOT NULL,
                        `toxins` TEXT NOT NULL,
                        `symptoms` TEXT NOT NULL,
                        `toxicParts` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_poisonous_family_genera_familyName` ON `poisonous_family_genera` (`familyName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_poisonous_family_genera_genusName` ON `poisonous_family_genera` (`genusName`)")
            }}
        }}
'''
        marker = re.search(r"\n\s*@Volatile\s*\n", text)
        if marker:
            text = text[:marker.start()] + migration_block + text[marker.start():]
        else:
            marker = re.search(r"\n\s*fun getDatabase\(", text)
            if not marker:
                raise SystemExit("ERROR: no encuentro dónde insertar la migración")
            text = text[:marker.start()] + migration_block + text[marker.start():]

        am = re.search(r"\.addMigrations\((.*?)\)", text, flags=re.S)
        if am:
            inside = am.group(1).strip()
            if migration_name not in inside:
                new_inside = inside + (", " if inside else "") + migration_name
                text = text[:am.start(1)] + new_inside + text[am.end(1):]
        else:
            raise SystemExit("ERROR: no encuentro .addMigrations(...). Añade manualmente " + migration_name)

    write_if_changed(path, text, original)


def patch_home_screen():
    path = ROOT / "app/src/main/java/com/toxicplants/database/ui/screens/HomeScreen.kt"
    original = path.read_text(encoding="utf-8")
    text = original

    if "onNavigateToPoisonousFamilies" not in text:
        # Firma de HomeScreen y firma de NavigationGrid: normalmente hay 2 apariciones.
        text = text.replace(
            "    onNavigateToLichens: () -> Unit = {},\n",
            "    onNavigateToLichens: () -> Unit = {},\n    onNavigateToPoisonousFamilies: () -> Unit = {},\n",
            2
        )

        # Llamada a NavigationGrid desde HomeScreen.
        text = re.sub(
            r"(\n\s*onNavigateToLichens\s*=\s*onNavigateToLichens,)",
            r"\1\n                    onNavigateToPoisonousFamilies = onNavigateToPoisonousFamilies,",
            text,
            count=1
        )

        card = '                        DialogOptionCard(gradient = Brush.horizontalGradient(listOf(Color(0xFF4A0E0E), Color(0xFFC62828))), icon = "☠️", title = "Familias venenosas", subtitle = "Familias, géneros, especies y ficha toxicológica editable", onClick  = { showBotanicaDialog = false; onNavigateToPoisonousFamilies() })'
        lines = text.splitlines()
        insert_after = None
        priorities = ["géneros tóxicos", "generos toxicos", "categorías", "categorias", "plantas"]
        for wanted in priorities:
            for i, line in enumerate(lines):
                low = line.lower()
                if "dialogoptioncard" in low and wanted in low:
                    insert_after = i
                    break
            if insert_after is not None:
                break
        if insert_after is None:
            raise SystemExit("ERROR: no encuentro la lista de Botánica para insertar Familias venenosas")
        lines.insert(insert_after + 1, card)
        text = "\n".join(lines) + ("\n" if original.endswith("\n") else "")

    write_if_changed(path, text, original)


def patch_main_activity():
    path = ROOT / "app/src/main/java/com/toxicplants/database/ui/MainActivity.kt"
    original = path.read_text(encoding="utf-8")
    text = original

    if "PoisonousFamilyViewModel" not in text:
        if "import com.toxicplants.database.ui.viewmodel.PlantViewModel" in text:
            text = text.replace(
                "import com.toxicplants.database.ui.viewmodel.PlantViewModel\n",
                "import com.toxicplants.database.ui.viewmodel.PlantViewModel\nimport com.toxicplants.database.ui.viewmodel.PoisonousFamilyViewModel\n",
                1
            )
        else:
            # Si ya usa import wildcard, no hace falta. Pero añadimos import explícito por seguridad.
            pkg_end = text.find("\n\n", text.find("package "))
            text = text[:pkg_end+2] + "import com.toxicplants.database.ui.viewmodel.PoisonousFamilyViewModel\n" + text[pkg_end+2:]

    if "val poisonousFamilyViewModel" not in text:
        text = re.sub(
            r"(\n\s*val\s+compoundViewModel[^\n]*=\s*viewModel\(\)\s*\n)",
            r"\1    val poisonousFamilyViewModel: PoisonousFamilyViewModel = viewModel()\n",
            text,
            count=1
        )

    if "onNavigateToPoisonousFamilies" not in text:
        # Mejor después de Líquenes; si no existe, después de Setas.
        new_arg = '                onNavigateToPoisonousFamilies = { navController.navigate("poisonous_families") },'
        lines = text.splitlines()
        inserted = False
        for target in ["onNavigateToLichens", "onNavigateToMushrooms"]:
            for i, line in enumerate(lines):
                if target in line and "navController.navigate" in line:
                    lines.insert(i + 1, new_arg)
                    inserted = True
                    break
            if inserted:
                break
        if not inserted:
            raise SystemExit("ERROR: no encuentro dónde añadir onNavigateToPoisonousFamilies en HomeScreen()")
        text = "\n".join(lines) + ("\n" if original.endswith("\n") else "")

    if 'composable("poisonous_families")' not in text:
        routes = r'''

        // ── FAMILIAS VENENOSAS ───────────────────────────────────────
        composable("poisonous_families") {
            PoisonousFamiliesScreen(
                viewModel = poisonousFamilyViewModel,
                onBack = { navController.popBackStack() },
                onFamilyClick = { familyName -> navController.navigate("poisonous_family/${Uri.encode(familyName)}") },
                onAddGenus = { navController.navigate("poisonous_genus/0/__new__") }
            )
        }

        composable(
            "poisonous_family/{familyName}",
            arguments = listOf(navArgument("familyName") { type = NavType.StringType })
        ) { backStackEntry ->
            val familyName = Uri.decode(backStackEntry.arguments?.getString("familyName") ?: "")
            PoisonousFamilyGeneraScreen(
                familyName = familyName,
                viewModel = poisonousFamilyViewModel,
                onBack = { navController.popBackStack() },
                onGenusClick = { genusId -> navController.navigate("poisonous_genus/$genusId/${Uri.encode(familyName)}") },
                onAddGenus = { fam -> navController.navigate("poisonous_genus/0/${Uri.encode(fam)}") }
            )
        }

        composable(
            "poisonous_genus/{genusId}/{familyName}",
            arguments = listOf(
                navArgument("genusId") { type = NavType.IntType },
                navArgument("familyName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val genusId = backStackEntry.arguments?.getInt("genusId") ?: 0
            val familyArg = backStackEntry.arguments?.getString("familyName") ?: "__new__"
            val initialFamilyName = if (familyArg == "__new__") "" else Uri.decode(familyArg)
            PoisonousGenusEditScreen(
                genusId = genusId,
                initialFamilyName = initialFamilyName,
                viewModel = poisonousFamilyViewModel,
                onBack = { navController.popBackStack() },
                onSaved = { savedFamilyName ->
                    navController.navigate("poisonous_family/${Uri.encode(savedFamilyName)}") {
                        popUpTo("poisonous_families") { inclusive = false }
                    }
                }
            )
        }
'''
        marker = re.search(r"\n\s*//\s*──\s*LISTA DE PLANTAS", text)
        if not marker:
            marker = re.search(r"\n\s*composable\(\"plant_list\"\)", text)
        if not marker:
            raise SystemExit("ERROR: no encuentro dónde insertar las rutas poisonous_families")
        text = text[:marker.start()] + routes + text[marker.start():]

    write_if_changed(path, text, original)


patch_plant_database()
patch_home_screen()
patch_main_activity()

print("\nOK: parche aplicado. Ahora ejecuta: ./gradlew clean assembleDebug")
PY

echo "==> Hecho. Ejecuta ahora: ./gradlew clean assembleDebug"
