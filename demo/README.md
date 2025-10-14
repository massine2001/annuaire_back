# Annuaire Backend

Application Spring Boot pour la gestion de pools de fichiers avec authentification JWT et stockage SFTP.

## 🚀 Fonctionnalités

- **Authentification JWT** avec cookies HttpOnly sécurisés
- **Gestion de pools** avec système de rôles (owner/admin/member)
- **Stockage SFTP** pour les fichiers
- **Système d'invitation** par token JWT
- **Statistiques** détaillées par pool
- **Preview de fichiers** (PDF, images, vidéos, audio)

## 📋 Prérequis

- **Java 21** ou supérieur
- **Maven 3.6+**
- **MySQL 8.0+**
- **Serveur SFTP** accessible


## 📚 API Endpoints

### Authentification
- `POST /api/auth/register` - Inscription
- `POST /api/auth/login` - Connexion
- `POST /api/auth/logout` - Déconnexion
- `GET /api/auth/me` - Utilisateur courant

### Pools
- `GET /api/pool/` - Liste des pools
- `POST /api/pool/` - Créer un pool
- `GET /api/pool/{id}` - Détails d'un pool
- `PUT /api/pool/{id}` - Modifier un pool
- `DELETE /api/pool/{id}` - Supprimer un pool
- `GET /api/pool/stats/{id}` - Statistiques du pool

### Fichiers
- `GET /api/files` - Liste des fichiers accessibles
- `POST /api/files/upload` - Upload un fichier
- `GET /api/files/download/{id}` - Télécharger un fichier
- `GET /api/files/preview/{id}` - Prévisualiser un fichier
- `PUT /api/files/{id}` - Modifier un fichier
- `DELETE /api/files/{id}` - Supprimer un fichier

### Invitations
- `POST /api/pool/invitations/generate-token` - Générer un lien d'invitation
- `GET /api/pool/invitations/validate/{token}` - Valider un token
- `POST /api/pool/invitations/accept` - Accepter une invitation
