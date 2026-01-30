package UniNest.Backend.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

public class FirebaseAuthService {

    public FirebaseToken verifyToken(String idToken) throws Exception {
        // throw an exception if token is invalid
        return FirebaseAuth.getInstance().verifyIdToken(idToken);
    }
}
