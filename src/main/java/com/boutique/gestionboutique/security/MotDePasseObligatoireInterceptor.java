package com.boutique.gestionboutique.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

// Bloquer toutes les pages tant que l'utilisateur n'a pas défini son propre mot de passe. */
public class MotDePasseObligatoireInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.getPrincipal() instanceof UtilisateurDetails utilisateur
                && utilisateur.isDoitChangerMotDePasse()) {
            response.sendRedirect(request.getContextPath() + "/profil/mot-de-passe");
            return false;
        }
        return true;
    }
}
