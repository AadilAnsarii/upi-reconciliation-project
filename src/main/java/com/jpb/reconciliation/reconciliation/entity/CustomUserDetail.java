//package com.jpb.reconciliation.reconciliation.entity;
//
//import java.util.Collection;
//
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//
//import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
//
//public class CustomUserDetail implements UserDetails {
//
//	private static final long serialVersionUID = 1L;
//	private ReconUser reconUser;
//
//	public CustomUserDetail(ReconUser reconUser) {
//		this.reconUser = reconUser;
//	}
//
//	@Override
//	public Collection<? extends GrantedAuthority> getAuthorities() {
//		return null;
//	}
//
//	@Override
//	public String getPassword() {
//		PasswordManager passwordManager = reconUser.getPasswordManager();
//		System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!-------------" + passwordManager.getUserPassword() );
//		return passwordManager.getUserPassword();
//	}
//
//	@Override
//	public String getUsername() {
//		return reconUser.getUsername();
//	}
//
//	@Override
//	public boolean isAccountNonExpired() {
//		// TODO Auto-generated method stub
//		return true;
//	}
//
//	@Override
//	public boolean isAccountNonLocked() {
//		// TODO Auto-generated method stub
//		return true;
//	}
//
//	@Override
//	public boolean isCredentialsNonExpired() {
//		// TODO Auto-generated method stub
//		return true;
//	}
//
//	@Override
//	public boolean isEnabled() {
//		// TODO Auto-generated method stub
//		return true;
//	}
//}
