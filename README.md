# ShareDL

Application Android qui reçoit un lien depuis le menu **Partager**, télécharge le média avec `yt-dlp`, en garde (par défaut) une copie locale, puis rouvre le sélecteur Android pour envoyer le fichier vers une autre application.

Pris en charge aujourd'hui : Instagram (posts, Reels, Stories), avec gestion de session pour les médias qui l'exigent. `yt-dlp` supporte un grand nombre d'autres sites ; l'app peut être étendue pour eux sans changer d'architecture.

## Fonctionnalités

- Réception d'un lien via le menu « Partager » de n'importe quelle application.
- Téléchargement en arrière-plan avec suivi de la progression.
- Copie locale optionnelle (activable/désactivable), listée sur l'écran d'accueil avec un bouton pour la repartager.
- Connexion Instagram via WebView lorsque le média l'exige, avec session réutilisée automatiquement pour les téléchargements suivants.

## Utilisation

1. Partage le lien d'une publication, d'un Reel ou d'une Story vers **ShareDL**.
2. Le téléchargement démarre automatiquement (la préférence « Enregistrer une copie sur le téléphone » est activée par défaut).
3. À la fin, le sélecteur Android s'ouvre avec le fichier vidéo.
4. Les copies conservées apparaissent sur l'écran d'accueil ; un bouton permet de repartager chacune d'elles.

Si Instagram exige une session, l'application ouvre sa page de connexion dans une WebView. Connecte-toi puis appuie sur **Utiliser cette session** : le téléchargement reprend automatiquement. Il est aussi possible d'ouvrir **Connecter Instagram** avant de partager un lien. **Déconnecter Instagram** supprime la session enregistrée par l'application.

## Stockage et confidentialité

- Les vidéos enregistrées sont dans le dossier privé externe de l'application : `Android/data/app.sharedl/files/Movies/ShareDL/`.
- Les cookies Instagram sont conservés sous forme de fichier Netscape dans le stockage privé de l'application, exclu des sauvegardes Android. Ils ne sont ni journalisés ni partagés avec d'autres applications.
- Si la copie locale est désactivée, la vidéo est placée temporairement dans le cache pour le partage, puis n'est pas conservée.

## Moteur de téléchargement

Le projet utilise `dev.ffmpegkit-maintained:yt-dlp-android:2.0.2`, qui embarque `yt-dlp` et Python. Les extracteurs dépendent des mécanismes d'accès des sites ciblés et peuvent cesser de fonctionner après un changement du site. Une connexion peut aider lorsque le média exige une session ; elle ne garantit pas le succès en cas de limitation de requêtes ou d'indisponibilité du média.

La dépendance fournit les ABI `arm64-v8a` et `x86_64` : les appareils Android 32 bits ne sont pas pris en charge. Elle augmente sensiblement la taille de l'application.

## Compilation

Ouvre le dossier dans Android Studio et lance la tâche Gradle `app > Tasks > build > assembleDebug`. Le SDK Android 35 est requis par la configuration actuelle (`minSdk` 26).

## Installer une version publiée

Chaque tag `vX.Y.Z` déclenche une build CI qui publie un APK signé sur la page [Releases](../../releases) du dépôt. Sur GrapheneOS, un outil comme Obtainium peut suivre ces Releases GitHub et proposer l'installation/mise à jour automatique de l'APK.

## Avertissement

Projet personnel destiné à un usage privé. Respecte les conditions d'utilisation des sites concernés et les droits des créateurs du contenu téléchargé.
