package com.mz.training.domains.user

import com.mz.training.domains.EntityId
import com.mz.training.domains.address.Address

/**
  * Created by zemi on 10/08/16.
  */
case class User(id: Long, firstName: String, lastName: String, addressId: Option[Long], address: Option[Address]) extends EntityId
