# Soclock - Mon Réveil Social

Application Android de réveil social développée avec **Kotlin** et **Jetpack Compose**, utilisant **Firebase** pour l'authentification et la synchronisation des données.

## Description

**Soclock** est une application de réveil collaborative qui permet de :
- Créer et gérer des alarmes personnalisées
- Inviter des amis à partager des alarmes
- Recevoir des notifications sociales (demandes d'ami, invitations d'alarme)
- Suivre les retards via Discord grâce à un webhook intégré

## Architecture

Le projet suit une architecture **MVVM** (Model-View-ViewModel) avec les couches suivantes :

```
app/src/main/java/com/example/filrouge/
├── MainActivity.kt              # Point d'entrée, DataStore thème
├── HomeScreen.kt                # Écran principal avec pager horizontal
├── alarms/                      # Module Alarmes
│   ├── Alarm.kt                 # Modèle de données (Firestore)
│   ├── AlarmPage.kt             # UI : liste, création, édition, sons
│   ├── AlarmViewModel.kt        # Logique métier + programmation système
│   ├── AlarmRepository.kt       # Accès Firestore (CRUD alarmes)
│   ├── AlarmReceiver.kt         # BroadcastReceiver : déclenchement alarme
│   ├── AlarmRingActivity.kt     # Écran plein écran quand l'alarme sonne
│   ├── AlarmInvitation.kt       # Modèle invitation d'alarme
│   ├── BootReceiver.kt          # Reprogrammation après redémarrage
│   └── SnoozeManager.kt         # Gestion snooze + webhook Discord
├── contacts/                    # Module Contacts
│   ├── AppUser.kt               # Modèle utilisateur
│   ├── ContactsPage.kt          # UI : liste de contacts, recherche
│   ├── ContactsViewModel.kt     # Logique métier contacts
│   ├── ContactsUiState.kt       # État UI contacts
│   ├── FriendRepository.kt      # Accès Firestore (amis)
│   └── FriendRequest.kt         # Modèle demande d'ami
├── navigation/                  # Navigation
│   ├── AppNavigation.kt         # NavHost et routes
│   └── Routes.kt                # Définition des routes
├── settings/                    # Paramètres & Profil
│   ├── SettingsPage.kt          # Page paramètres
│   ├── ProfileScreen.kt         # Écran profil utilisateur
│   ├── ProfileViewModel.kt      # Logique profil
│   └── ProfileUiState.kt        # État UI profil
├── auth/                        # Authentification
│   ├── AuthRepository.kt        # Firebase Auth (email + Google)
│   ├── LoginViewModel.kt        # Logique connexion
│   ├── SignUpScreen.kt          # Écran inscription
│   └── SignupViewModel.kt       # Logique inscription
└── ui/theme/                    # Thème Material3
```

## Fonctionnalités

### Alarmes
- **Création/Édition** d'alarmes avec nom, heure, jours de répétition
- **Choix de sonnerie** parmi 9 sons intégrés
- **Option "Je suis complètement crazy"** 🤪 : sélectionne un son aléatoire parmi les sons goofy à chaque déclenchement (l'utilisateur ne sait pas quel son va être joué)
- **Vibreur** activable/désactivable
- **Snooze** (+10 min) avec compteur de retard cumulé
- **Plein écran** au déclenchement (même sur écran verrouillé)
- **Reprogrammation automatique** après redémarrage de l'appareil
- **Fonctionne en mode avion**

### Contacts & Social
- **Recherche d'utilisateurs** inscrits sur l'application
- **Demandes d'ami** avec acceptation/refus
- **Invitations d'alarme** : inviter des contacts à partager une alarme
- **Système de notifications** intégré (demandes d'ami + invitations)

### Intégration Discord
- **Webhook Discord** configurable par alarme
- Notification automatique sur Discord à chaque snooze (avec nom, alarme et compteur)

### Paramètres
- **Thème sombre** persistant (DataStore)
- **Profil utilisateur** éditable

### Authentification
- Connexion par **email/mot de passe**
- Connexion via **Google** (Credential Manager)
- **Réinitialisation de mot de passe**
- **Inscription** avec nom d'affichage

## Technologies

| Technologie | Utilisation |
|---|---|
| **Kotlin** | Langage principal |
| **Jetpack Compose** | UI déclarative (Material3) |
| **Firebase Auth** | Authentification (email + Google) |
| **Cloud Firestore** | Base de données temps réel |
| **Firebase Messaging** | Notifications push |
| **Navigation Compose** | Navigation entre écrans |
| **DataStore** | Persistance préférences locales |
| **AlarmManager** | Programmation d'alarmes exactes |
| **MediaPlayer** | Lecture des sonneries |

## Installation

1. **Cloner le projet**
   ```bash
   git clone <url-du-repo>
   cd projet_fil_rouge
   ```

2. **Configurer Firebase**
   - Créer un projet sur [Firebase Console](https://console.firebase.google.com)
   - Activer Authentication (Email/Password + Google)
   - Créer une base Firestore
   - Télécharger `google-services.json` et le placer dans `app/`

## Ressources audio

Les sons d'alarme sont dans `app/src/main/res/raw/`.

Les sons goofy sont stockés dans  `app/src/main/res/raw/goofy`
Il est possible d'en ajouter à tout moment dans ce dossier et ils seront automatiquement pris en compte.

## Permissions Android

L'application utilise les permissions suivantes (déclarées dans `AndroidManifest.xml`) :

- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` — programmer des alarmes exactes
- `RECEIVE_BOOT_COMPLETED` — reprogrammer après redémarrage
- `VIBRATE` — vibration de l'alarme
- `POST_NOTIFICATIONS` — notifications d'alarme
- `USE_FULL_SCREEN_INTENT` — écran plein pendant l'alarme
- `WAKE_LOCK` — allumer l'écran
- `SYSTEM_ALERT_WINDOW` — affichage par-dessus les autres apps

## Navigation

L'application utilise un **HorizontalPager** avec 3 pages swipables :

1. **Contacts** (page 0) — Liste d'amis et ajout de contacts
2. **Alarmes** (page 1, page d'accueil) — Création et gestion des alarmes
3. **Paramètres** (page 2) — Thème, recherche, profil

Une barre de navigation en bas permet de basculer entre les pages.
