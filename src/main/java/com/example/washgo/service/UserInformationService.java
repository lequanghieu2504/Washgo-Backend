package com.example.washgo.service;

import com.example.washgo.model.UserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityNotFoundException;

import com.example.washgo.dtos.ClientProfileDTO;
import com.example.washgo.dtos.ClientProfileUpdateDTO;
import com.example.washgo.enums.GenderType;
import com.example.washgo.mapper.UserMapper;
import com.example.washgo.model.ClientProfile;
import com.example.washgo.model.UserInformation;
import com.example.washgo.repository.*;
@Service
public class UserInformationService {
	private UserInformationRepository userInformationRepository;
	private ClientProfileRepository clientProfileRepository;
	private UserAccountRepository userAccountRepository;
	
	@Autowired
	public UserInformationService(ClientProfileRepository clientProfileRepository,
								  UserInformationRepository userInformationRepository,
								  UserAccountRepository userAccountRepository) {
		this.userInformationRepository = userInformationRepository;
		this.clientProfileRepository = clientProfileRepository;
		this.userAccountRepository = userAccountRepository;
	}

	public ClientProfileDTO getClientProfile(Long userId) {
		ClientProfileDTO clientProfileDTO = new ClientProfileDTO();
		UserInformation userInformation = findUserById(userId);
		
		clientProfileDTO = UserMapper.toClientProfileDTO(userInformation);
		
		return clientProfileDTO;
	}
	
	public UserInformation findUserById(Long userId) {
	    return userInformationRepository.findById(userId)
	    		.orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
	}
	public ClientProfile findClientById(Long userId) {
	    return clientProfileRepository.findById(userId)
	    		.orElseThrow(() -> new EntityNotFoundException("Client not found with id: " + userId));
	}

	public boolean updateClientProfile(ClientProfileUpdateDTO updateDTO) {
				ClientProfile clientProfile = findClientById(updateDTO.getUserId());
				UserInformation userInformation = findUserById(clientProfile.getId());
				UserAccount userAccount = userInformation.getAccount();

				if(updateDTO.getUserName()!=null) {
					userAccount = userInformation.getAccount();
					userAccount.setUsername(updateDTO.getUserName());
				}
				if(updateDTO.getBirthDay()!=null) {
					clientProfile.setBirthDay(updateDTO.getBirthDay());
				}
				if(updateDTO.getGender() !=  null) {
					clientProfile.setGender(GenderType.valueOf(updateDTO.getGender()));
				}
		ClientProfile savedClientProfile = null;
		UserAccount savedUserAccount = null;
				if(userAccount!=null) {
					 savedClientProfile = clientProfileRepository.save(clientProfile);
				}
				if(userAccount!=null) {
					savedUserAccount = userAccountRepository.save(userAccount);
				}
				if (savedClientProfile != null && savedUserAccount != null) {
					return true;	
				}
		return false;
	}

	
}
