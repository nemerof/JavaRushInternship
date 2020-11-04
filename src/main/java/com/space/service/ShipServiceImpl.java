package com.space.service;

import com.space.controller.ShipOrder;
import com.space.model.Ship;
import com.space.model.ShipType;
import com.space.repository.ShipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class ShipServiceImpl implements ShipService {
    private ShipRepository shipRepository;

    public ShipServiceImpl() {
    }

    @Autowired
    public ShipServiceImpl(ShipRepository shipRepository) {
        super();
        this.shipRepository = shipRepository;
    }

    @Override
    public Ship saveShip(Ship ship) {
        return shipRepository.save(ship);
    }

    @Override
    public Ship getShip(Long id) {
        return shipRepository.findById(id).orElse(null);
    }

    @Override
    public Ship updateShip(Ship prevShip, Ship newShip) {
        boolean changeRating = false;

        String name = newShip.getName();
        if (name != null) {
            if (isStringValid(name)) {
                prevShip.setName(name);
            } else {
                throw new IllegalArgumentException("name");
            }
        }

        String planet = newShip.getPlanet();
        if (planet != null) {
            if (isStringValid(planet)) {
                prevShip.setPlanet(planet);
            } else {
                throw new IllegalArgumentException("planet");
            }
        }

        ShipType type = newShip.getShipType();
        if (type != null) {
            prevShip.setShipType(type);
        }

        Date prod = newShip.getProdDate();
        if (prod != null) {
            if (isProdDateValid(prod)) {
                prevShip.setProdDate(prod);
                changeRating = true;
            } else {
                throw new IllegalArgumentException("date");
            }
        }

        Boolean isUsed = newShip.getUsed();
        if (isUsed != null) {
            prevShip.setUsed(isUsed);
            changeRating = true;
        }

        Double speed = newShip.getSpeed();
        if (speed != null) {
            if (isSpeedValid(speed)) {
                prevShip.setSpeed(speed);
                changeRating = true;
            } else {
                throw new IllegalArgumentException("speed");
            }
        }

        Integer size = newShip.getCrewSize();
        if (size != null) {
            if (isCrewSizeValid(size)) {
                prevShip.setCrewSize(size);
            } else {
                throw new IllegalArgumentException("size");
            }
        }

        if (changeRating) {
            prevShip.setRating(countRating(prevShip.getSpeed(), prevShip.getUsed(), prevShip.getProdDate()));
        }

        shipRepository.save(prevShip);
        return prevShip;
    }

    @Override
    public void deleteShip(Ship ship) {
        shipRepository.delete(ship);
    }

    @Override
    public List<Ship> getShips(String name, String planet, ShipType shipType, Long after, Long before, Boolean isUsed, Double minSpeed, Double maxSpeed, Integer minCrewSize, Integer maxCrewSize, Double minRating, Double maxRating) {
        final List<Ship> list = new ArrayList<>();
        final Date afterDate = after == null ? null : new Date(after);
        final Date beforeDate = before == null ? null : new Date(before);

        shipRepository.findAll().forEach((ship) -> {
            if (name != null && !ship.getName().contains(name)) return;
            if (planet != null && !ship.getPlanet().contains(planet)) return;
            if (shipType != null && ship.getShipType() != shipType) return;
            if (afterDate != null && !ship.getProdDate().after(afterDate)) return;
            if (beforeDate != null && !ship.getProdDate().before(beforeDate)) return;
            if (isUsed != null && ship.getUsed().booleanValue() != isUsed.booleanValue()) return;
            if (minSpeed != null && ship.getSpeed().compareTo(minSpeed) < 0) return;
            if (maxSpeed != null && ship.getSpeed().compareTo(maxSpeed) > 0) return;
            if (minCrewSize != null && ship.getCrewSize().compareTo(minCrewSize) < 0) return;
            if (maxCrewSize != null && ship.getCrewSize().compareTo(maxCrewSize) > 0) return;
            if (minRating != null && ship.getRating().compareTo(minRating) < 0) return;
            if (maxRating != null && ship.getRating().compareTo(maxRating) > 0) return;

            list.add(ship);
        });

        return list;
    }

    @Override
    public List<Ship> sortShips(List<Ship> ships, ShipOrder order) {
        if (order != null) {
            ships.sort((ship1, ship2) -> {
                switch (order) {
                    case ID: return ship1.getId().compareTo(ship2.getId());
                    case SPEED: return ship1.getSpeed().compareTo(ship2.getSpeed());
                    case DATE: return ship1.getProdDate().compareTo(ship2.getProdDate());
                    case RATING: return ship1.getRating().compareTo(ship2.getRating());
                    default: return 0;
                }
            });
        }
        return ships;
    }

    @Override
    public List<Ship> getPage(List<Ship> ships, Integer pageNumber, Integer pageSize) {
        final Integer page = pageNumber == null ? 0 : pageNumber;
        final Integer size = pageSize == null ? 3 : pageSize;
        final int from = page * size;

        int to = from + size;
        if (to > ships.size())
            to = ships.size();

        return ships.subList(from, to);
    }

    @Override
    public boolean isShipValid(Ship ship) {
        return ship != null && isStringValid(ship.getName())
                && isProdDateValid(ship.getProdDate())
                && isStringValid(ship.getPlanet())
                && isSpeedValid(ship.getSpeed())
                && isCrewSizeValid(ship.getCrewSize());
    }

    @Override
    public double countRating(double speed, boolean isUsed, Date prod) {
        final double k = isUsed ? 0.5d : 1;
        final int y0 = 3019;

        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(prod);
        final int y1 = calendar.get(Calendar.YEAR);
        final double rating = (80 * speed * k)
                            / (y0 - y1 + 1);

        return Math.round(rating * 100) / 100d;
    }

    private boolean isProdDateValid(Date prodDate) {
        final Date startProd = getDateForYear(2800);
        final Date endProd = getDateForYear(3019);
        return prodDate != null && prodDate.after(startProd) && prodDate.before(endProd);
    }

    private Date getDateForYear(int year) {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        return calendar.getTime();
    }

    private boolean isCrewSizeValid(Integer crewSize) {
        return crewSize != null && crewSize >= 1 && crewSize <= 9999;
    }

    private boolean isSpeedValid(Double speed) {
        return speed != null && speed >= 0.01d && speed <= 0.99d;
    }

    private boolean isStringValid(String value) {
        return value != null && !value.isEmpty() && value.length() <= 50;
    }
}
