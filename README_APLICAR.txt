PARCHE v2: Familias venenosas
=============================

Este ZIP está preparado para descomprimir DIRECTAMENTE dentro de la raíz de tu proyecto PlantasToxicas.
Al descomprimir verás en la raíz:

- new_files/
- aplicar_familias_venenosas.sh
- README_APLICAR.txt

Uso:

cd ~/AndroidStudioProjects/PlantasToxicas
unzip -o ~/Descargas/FamiliasVenenosas_patch_v2_root.zip
bash aplicar_familias_venenosas.sh
./gradlew clean assembleDebug

El script crea copias .bak_familias_venenosas antes de modificar archivos existentes.
