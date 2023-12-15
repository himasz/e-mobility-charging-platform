package com.dcs.service;

import com.dcs.common.constants.Constants;
import com.dcs.common.dto.ChargeDetailDTO;
import com.dcs.common.entity.ChargeDetailEntity;
import com.dcs.common.error.exceptions.DataNotFoundException;
import com.dcs.common.error.exceptions.DataValidationException;
import com.dcs.mapper.ChargeDetailMapper;
import com.dcs.repository.ChargeDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class ChargeDetailServiceImpl implements ChargeDetailService {
    private final ChargeDetailRepository repository;
    private final ChargeDetailMapper chargeDetailMapper;
    private static final AtomicLong atomicOldEndTime = new AtomicLong();

    @Override
    public void createChargeDetailRecord(final ChargeDetailDTO chargeDetailDTO) {
        checkTimeConstraint(chargeDetailDTO.getEndTime(), chargeDetailDTO.getStartTime());
        repository.save(chargeDetailMapper.toEntity(chargeDetailDTO));
    }

    @Override
    public ChargeDetailDTO getChargeDetailRecord(final long chargeId) {
        ChargeDetailEntity entity = Optional.of(repository.getById(chargeId))
                .orElseThrow(() -> new DataNotFoundException("No charge detail found for: " + chargeId));
        return chargeDetailMapper.toDTO(entity);
    }

    @Override
    public List<ChargeDetailEntity> searchVehicleChargeDetails(final String vin, final Integer page, final Integer pageSize) {
        List<ChargeDetailEntity> chargeDetailEntities =
                repository.getAllChargeDetailByVin(vin, getPageRequest(page, pageSize));
        if (chargeDetailEntities.isEmpty()) {
            throw new DataNotFoundException("vin = " + vin);
        }
        return chargeDetailEntities;
    }

    private static PageRequest getPageRequest(Integer page, Integer pageSize) {
        Sort sort = Sort.by(Sort.Direction.ASC, Constants.START_TIME)
                .and(Sort.by(Sort.Direction.ASC, Constants.END_TIME));
        return PageRequest.of(page, pageSize, sort);
    }

    private static void checkTimeConstraint(final long endTime, final long startTime) {
        boolean isAfterLastCharge = atomicOldEndTime.get() != 0 && atomicOldEndTime.get() > startTime;
        if (startTime >= endTime && isAfterLastCharge) {
            throw new DataValidationException(String.format("startTime: %s , endTime: %s", startTime, endTime));
        }
        atomicOldEndTime.set(endTime);
    }
}