package com.mz.training.domains.address

import com.mz.training.domains.EntityId

/**
  * Created by zemi on 10/08/16.
  */
case class Address(id: Long, street: String, zip: String, houseNumber: String, city: String) extends EntityId
