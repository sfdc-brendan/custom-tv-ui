"""Small dependency-free Android build using Google's SDK tools and JDK 21."""
from pathlib import Path
import os, subprocess, zipfile

ROOT = Path(__file__).resolve().parent
SDK = Path(os.environ.get('FIRETV_SDK', '/private/tmp/firetv-tools/sdk'))
TOOLS = SDK / 'android-15'
ANDROID = SDK / 'android-7.1.1/android.jar'
JDK = Path('/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home')
BUILD = ROOT / 'build'
BUILD.mkdir(exist_ok=True)
(BUILD / 'classes').mkdir(exist_ok=True)
(BUILD / 'dex').mkdir(exist_ok=True)
env = dict(os.environ, JAVA_HOME=str(JDK), PATH=str(JDK/'bin')+os.pathsep+os.environ.get('PATH',''))
def run(*args):
    subprocess.run([str(a) for a in args], cwd=ROOT, env=env, check=True)

run(TOOLS/'aapt', 'package', '-f', '-M', ROOT/'AndroidManifest.xml', '-S', ROOT/'res', '-I', ANDROID, '-F', BUILD/'unsigned.apk')
run(JDK/'bin/javac', '-source', '8', '-target', '8', '-encoding', 'UTF-8', '-classpath', ANDROID, '-d', BUILD/'classes', *ROOT.glob('src/**/*.java'))
run(TOOLS/'d8', '--min-api', '25', '--lib', ANDROID, '--output', BUILD/'dex', *BUILD.glob('classes/**/*.class'))
with zipfile.ZipFile(BUILD/'unsigned.apk', 'a', zipfile.ZIP_DEFLATED) as z:
    z.write(BUILD/'dex/classes.dex', 'classes.dex')
run(TOOLS/'zipalign', '-f', '4', BUILD/'unsigned.apk', BUILD/'aligned.apk')
key = BUILD/'development.keystore'
if not key.exists():
    run(JDK/'bin/keytool', '-genkeypair', '-keystore', key, '-storepass', 'android', '-keypass', 'android', '-alias', 'development', '-dname', 'CN=Local Command Center', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '10000')
run(TOOLS/'apksigner', 'sign', '--ks', key, '--ks-pass', 'pass:android', '--key-pass', 'pass:android', '--out', BUILD/'command-center.apk', BUILD/'aligned.apk')
run(TOOLS/'apksigner', 'verify', '--verbose', BUILD/'command-center.apk')
print('Built', BUILD/'command-center.apk')
